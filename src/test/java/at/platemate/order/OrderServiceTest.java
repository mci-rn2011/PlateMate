package at.platemate.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import at.platemate.cart.CartLine;
import at.platemate.cart.CartService;
import at.platemate.delivery.GeoPoint;
import at.platemate.delivery.GeocodedLocation;
import at.platemate.delivery.LocationService;
import at.platemate.delivery.RouteEstimate;
import at.platemate.delivery.RouteService;
import at.platemate.menu.MenuItem;
import at.platemate.payment.Payment;
import at.platemate.payment.PaymentService;
import at.platemate.payment.PaymentStatus;
import at.platemate.restaurant.Restaurant;
import at.platemate.restaurant.RestaurantRepository;
import at.platemate.user.Role;
import at.platemate.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderEventBroadcaster orderEventBroadcaster;

    @Mock
    private LocationService locationService;

    @Mock
    private RouteService routeService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                restaurantRepository,
                paymentService,
                orderEventBroadcaster,
                locationService,
                routeService);
    }

    @Test
    void placeOrderWithApprovedPaymentCreatesPlacedOrderAndClearsCart() {
        User customer = new User("Customer", Role.CUSTOMER);
        Restaurant restaurant = new Restaurant("Plate House", "Fresh meals", "Bowls", "Main Street 1", true, null);
        restaurant.setCoordinates(47.27, 11.39);
        MenuItem menuItem = new MenuItem(restaurant, "Falafel Bowl", "With hummus", new BigDecimal("12.50"), true);
        CartService cartService = org.mockito.Mockito.mock(CartService.class);
        OrderService.CheckoutDetails details = new OrderService.CheckoutDetails(
                "Casey",
                "+43 660 123456",
                "casey@example.test",
                "Campus Alley 4",
                "6020",
                "Innsbruck",
                "Leave at reception");

        when(cartService.isEmpty()).thenReturn(false);
        when(cartService.getRestaurantId()).thenReturn(Optional.of(42L));
        when(cartService.getTotal()).thenReturn(new BigDecimal("25.00"));
        when(cartService.getLines()).thenReturn(List.of(new CartLine(menuItem, 2)));
        when(restaurantRepository.findById(42L)).thenReturn(Optional.of(restaurant));
        when(locationService.forwardGeocode("Campus Alley 4, 6020, Innsbruck"))
                .thenReturn(Optional.of(new GeocodedLocation(new GeoPoint(47.26, 11.40), "Campus Alley 4, Innsbruck")));
        when(routeService.estimateRoute(any(), any()))
                .thenReturn(new RouteEstimate(new BigDecimal("4.20"), 14, "/route-preview.svg"));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentService.approve(any(CustomerOrder.class))).thenAnswer(invocation -> new Payment(
                invocation.getArgument(0),
                new BigDecimal("25.00"),
                PaymentStatus.APPROVED,
                "mock-approved"));

        CustomerOrder order = orderService.placeOrder(customer, cartService, true, details);

        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(customer, order.getCustomer());
        assertEquals(restaurant, order.getRestaurant());
        assertEquals(new BigDecimal("25.00"), order.getTotalPrice());
        assertEquals(1, order.getItems().size());
        assertEquals("Campus Alley 4, Innsbruck", order.getDeliveryAddressNormalized());
        assertEquals("/route-preview.svg", order.getRoutePreviewUrl());
        verify(cartService).clear();
        verify(paymentService).approve(order);
        verify(paymentService, never()).decline(any());
        verify(orderEventBroadcaster).publish(order);
    }

    @Test
    void acceptOrderMovesPlacedOrderToPreparing() {
        CustomerOrder order = orderWithStatus(OrderStatus.PLACED);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        orderService.acceptOrder(7L);

        assertEquals(OrderStatus.PREPARING, order.getStatus());
        verify(orderEventBroadcaster).publish(order);
    }

    @Test
    void markReadyRejectsOrdersThatAreNotPreparing() {
        CustomerOrder order = orderWithStatus(OrderStatus.PLACED);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.markReady(7L));

        assertEquals("Expected PREPARING but was PLACED", exception.getMessage());
        assertEquals(OrderStatus.PLACED, order.getStatus());
        verify(orderEventBroadcaster, never()).publish(any(CustomerOrder.class));
    }

    @Test
    void cancelOrderRequiresCancellationNote() {
        CustomerOrder order = orderWithStatus(OrderStatus.PREPARING);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.cancelOrder(7L, " "));

        assertEquals("Cancellation note is required.", exception.getMessage());
        assertEquals(OrderStatus.PREPARING, order.getStatus());
        verify(orderEventBroadcaster, never()).publish(any(CustomerOrder.class));
    }

    private CustomerOrder orderWithStatus(OrderStatus status) {
        CustomerOrder order = new CustomerOrder(
                new User("Customer", Role.CUSTOMER),
                new Restaurant("Plate House", "Fresh meals", "Bowls", "Main Street 1", true, null),
                BigDecimal.TEN);
        order.setStatus(status);
        return order;
    }
}
