package at.platemate.order;

import java.util.List;

import at.platemate.cart.CartLine;
import at.platemate.cart.CartService;
import at.platemate.delivery.GeocodedLocation;
import at.platemate.delivery.GeoPoint;
import at.platemate.delivery.LocationService;
import at.platemate.delivery.RouteEstimate;
import at.platemate.delivery.RouteService;
import at.platemate.payment.Payment;
import at.platemate.payment.PaymentService;
import at.platemate.payment.PaymentStatus;
import at.platemate.restaurant.Restaurant;
import at.platemate.restaurant.RestaurantRepository;
import at.platemate.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final PaymentService paymentService;
    private final OrderEventBroadcaster orderEventBroadcaster;
    private final LocationService locationService;
    private final RouteService routeService;

    public OrderService(
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            PaymentService paymentService,
            OrderEventBroadcaster orderEventBroadcaster,
            LocationService locationService,
            RouteService routeService) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.paymentService = paymentService;
        this.orderEventBroadcaster = orderEventBroadcaster;
        this.locationService = locationService;
        this.routeService = routeService;
    }

    @Transactional
    public CustomerOrder placeOrder(User customer, CartService cartService, boolean paymentApproved) {
        return placeOrder(customer, cartService, paymentApproved, CheckoutDetails.empty());
    }

    @Transactional
    public CustomerOrder placeOrder(
            User customer,
            CartService cartService,
            boolean paymentApproved,
            CheckoutDetails checkoutDetails) {
        return placeOrder(customer, cartService, paymentApproved, checkoutDetails, null);
    }

    @Transactional
    public CustomerOrder placeGuestOrder(
            String guestSessionId,
            CartService cartService,
            boolean paymentApproved,
            CheckoutDetails checkoutDetails) {
        return placeGuestOrder(null, guestSessionId, cartService, paymentApproved, checkoutDetails);
    }

    @Transactional
    public CustomerOrder placeGuestOrder(
            User guestUser,
            String guestSessionId,
            CartService cartService,
            boolean paymentApproved,
            CheckoutDetails checkoutDetails) {
        if (guestSessionId == null || guestSessionId.isBlank()) {
            throw new IllegalArgumentException("Guest session id is required.");
        }
        return placeOrder(guestUser, cartService, paymentApproved, checkoutDetails, guestSessionId);
    }

    private CustomerOrder placeOrder(
            User customer,
            CartService cartService,
            boolean paymentApproved,
            CheckoutDetails checkoutDetails,
            String guestSessionId) {
        if (cartService.isEmpty()) {
            throw new IllegalStateException("Cannot place an empty order.");
        }

        Restaurant restaurant = restaurantRepository.findById(cartService.getRestaurantId().orElseThrow())
                .orElseThrow(() -> new IllegalStateException("Cart restaurant no longer exists."));
        CheckoutDetails details = checkoutDetails == null ? CheckoutDetails.empty() : checkoutDetails;
        CustomerOrder order = new CustomerOrder(
                customer,
                restaurant,
                cartService.getTotal(),
                guestSessionId,
                details.contactName(),
                details.contactPhone(),
                details.contactEmail(),
                details.deliveryAddress(),
                details.deliveryPostalCode(),
                details.deliveryCity(),
                details.deliveryInstructions());
        enrichDeliveryLocation(order);

        for (CartLine line : cartService.getLines()) {
            order.addItem(new OrderItem(
                    line.getMenuItem().getId(),
                    line.getMenuItem().getName(),
                    line.getQuantity(),
                    line.getMenuItem().getPrice()));
        }

        CustomerOrder saved = orderRepository.save(order);
        Payment payment = paymentApproved ? paymentService.approve(saved) : paymentService.decline(saved);
        saved.setStatus(payment.getStatus() == PaymentStatus.APPROVED ? OrderStatus.PLACED : OrderStatus.CANCELLED);
        cartService.clear();
        orderEventBroadcaster.publish(saved);
        return saved;
    }

    private void enrichDeliveryLocation(CustomerOrder order) {
        locationService.forwardGeocode(order.getFullDeliveryAddress()).ifPresent(location -> {
            order.setDeliveryLatitude(location.coordinates().latitude());
            order.setDeliveryLongitude(location.coordinates().longitude());
            order.setDeliveryAddressNormalized(location.normalizedAddress());
        });
        RouteEstimate route = routeService.estimateRoute(restaurantPoint(order), deliveryPoint(order));
        order.setRoutePreviewUrl(route.previewUrl());
    }

    private java.util.Optional<GeoPoint> restaurantPoint(CustomerOrder order) {
        Restaurant restaurant = order.getRestaurant();
        if (restaurant == null) {
            return java.util.Optional.empty();
        }
        if (restaurant.getLatitude() != null && restaurant.getLongitude() != null) {
            return java.util.Optional.of(new GeoPoint(restaurant.getLatitude(), restaurant.getLongitude()));
        }
        return locationService.forwardGeocode(restaurant.getAddress())
                .map(GeocodedLocation::coordinates);
    }

    private java.util.Optional<GeoPoint> deliveryPoint(CustomerOrder order) {
        if (order.getDeliveryLatitude() != null && order.getDeliveryLongitude() != null) {
            return java.util.Optional.of(new GeoPoint(order.getDeliveryLatitude(), order.getDeliveryLongitude()));
        }
        return locationService.forwardGeocode(order.getFullDeliveryAddress())
                .map(GeocodedLocation::coordinates);
    }

    public List<CustomerOrder> findCustomerOrders(User customer) {
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<CustomerOrder> findGuestSessionOrders(String guestSessionId) {
        if (guestSessionId == null || guestSessionId.isBlank()) {
            return List.of();
        }
        return orderRepository.findByGuestSessionIdOrderByCreatedAtDesc(guestSessionId.trim());
    }

    public List<CustomerOrder> findRestaurantOrders(Restaurant restaurant) {
        return orderRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    public List<CustomerOrder> findRestaurantOrders(List<Restaurant> restaurants) {
        if (restaurants.isEmpty()) {
            return List.of();
        }
        return orderRepository.findByRestaurantInOrderByCreatedAtDesc(restaurants);
    }

    public List<CustomerOrder> findDriverOrders(User driver) {
        return orderRepository.findByAssignedDriverOrderByCreatedAtDesc(driver);
    }

    public CustomerOrder getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Transactional
    public void acceptOrder(Long orderId) {
        CustomerOrder order = getOrder(orderId);
        requireStatus(order, OrderStatus.PLACED);
        order.setStatus(OrderStatus.PREPARING);
        orderEventBroadcaster.publish(order);
    }

    @Transactional
    public void rejectOrder(Long orderId) {
        CustomerOrder order = getOrder(orderId);
        requireStatus(order, OrderStatus.PLACED);
        order.setStatus(OrderStatus.REJECTED);
        order.setAssignedDriver(null);
        orderEventBroadcaster.publish(order);
    }

    @Transactional
    public void rejectOrder(Long orderId, String cancellationNote) {
        CustomerOrder order = getOrder(orderId);
        requireStatus(order, OrderStatus.PLACED);
        requireCancellationNote(cancellationNote);
        order.setCancellationNote(cancellationNote);
        order.setStatus(OrderStatus.REJECTED);
        order.setAssignedDriver(null);
        orderEventBroadcaster.publish(order);
    }

    @Transactional
    public void markPreparing(Long orderId) {
        CustomerOrder order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.PREPARING) {
            return;
        }
        requireStatus(order, OrderStatus.ACCEPTED);
        order.setStatus(OrderStatus.PREPARING);
        orderEventBroadcaster.publish(order);
    }

    @Transactional
    public void markReady(Long orderId) {
        CustomerOrder order = getOrder(orderId);
        requireStatus(order, OrderStatus.PREPARING);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        orderEventBroadcaster.publish(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, String cancellationNote) {
        CustomerOrder order = getOrder(orderId);
        requireCancellableBeforePickup(order);
        requireCancellationNote(cancellationNote);
        order.setCancellationNote(cancellationNote);
        order.setAssignedDriver(null);
        order.setStatus(OrderStatus.CANCELLED);
        orderEventBroadcaster.publish(order);
    }

    private void requireStatus(CustomerOrder order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new IllegalStateException("Expected " + expected + " but was " + order.getStatus());
        }
    }

    private void requireCancellableBeforePickup(CustomerOrder order) {
        if (order.getStatus() == OrderStatus.READY_FOR_PICKUP
                || order.getStatus() == OrderStatus.OUT_FOR_DELIVERY
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order can only be cancelled before it is ready for pickup.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
            throw new IllegalStateException("Order is already closed.");
        }
    }

    private void requireCancellationNote(String cancellationNote) {
        if (cancellationNote == null || cancellationNote.isBlank()) {
            throw new IllegalArgumentException("Cancellation note is required.");
        }
    }

    public record CheckoutDetails(
            String contactName,
            String contactPhone,
            String contactEmail,
            String deliveryAddress,
            String deliveryPostalCode,
            String deliveryCity,
            String deliveryInstructions) {

        public static CheckoutDetails empty() {
            return new CheckoutDetails(null, null, null, null, null, null, null);
        }
    }
}
