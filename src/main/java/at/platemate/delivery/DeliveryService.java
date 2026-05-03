package at.platemate.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import at.platemate.order.CustomerOrder;
import at.platemate.order.OrderEventBroadcaster;
import at.platemate.order.OrderRepository;
import at.platemate.order.OrderStatus;
import at.platemate.user.Role;
import at.platemate.user.User;
import at.platemate.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private static final List<DeliveryStatus> ACTIVE_DELIVERY_STATUSES = List.of(
            DeliveryStatus.ACCEPTED_BY_DRIVER,
            DeliveryStatus.PICKED_UP,
            DeliveryStatus.ON_THE_WAY);
    private static final BigDecimal DELIVERY_FEE_FLAT_EUR = new BigDecimal("2.00");
    private static final BigDecimal DELIVERY_FEE_PER_KM_EUR = new BigDecimal("0.35");

    private final DeliveryRepository deliveryRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderEventBroadcaster orderEventBroadcaster;
    private final LocationService locationService;
    private final RouteService routeService;

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            DriverProfileRepository driverProfileRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            OrderEventBroadcaster orderEventBroadcaster,
            LocationService locationService,
            RouteService routeService) {
        this.deliveryRepository = deliveryRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderEventBroadcaster = orderEventBroadcaster;
        this.locationService = locationService;
        this.routeService = routeService;
    }

    public List<User> findDrivers() {
        return findAvailableAssignableDrivers();
    }

    public List<User> findAvailableAssignableDrivers() {
        return userRepository.findByRole(Role.DRIVER).stream()
                .filter(driver -> {
                    DriverProfile profile = ensureDriverProfile(driver);
                    refreshAutomaticStatus(profile);
                    return profile.getStatus() == DriverStatus.AVAILABLE
                            && activeDeliveryCount(driver) < profile.getActiveDeliveryLimit();
                })
                .toList();
    }

    public DriverProfile getDriverProfile(User driver) {
        return ensureDriverProfile(driver);
    }

    @Transactional
    public DriverProfile updateDriverAvailability(User driver, DriverStatus requestedStatus) {
        DriverProfile profile = ensureDriverProfile(driver);
        if (requestedStatus == DriverStatus.BUSY) {
            throw new IllegalArgumentException("BUSY is set automatically when the active delivery limit is reached.");
        }
        profile.setStatus(requestedStatus);
        refreshAutomaticStatus(profile);
        DriverProfile saved = driverProfileRepository.save(profile);
        orderEventBroadcaster.publishGlobal();
        return saved;
    }

    public List<Delivery> findDeliveries(User driver) {
        return deliveryRepository.findByDriverOrderByAssignedAtDesc(driver);
    }

    public List<Delivery> findActiveDeliveries(User driver) {
        return deliveryRepository.findByDriverAndStatusInOrderByAssignedAtDesc(driver, ACTIVE_DELIVERY_STATUSES);
    }

    public Optional<Delivery> findByOrder(CustomerOrder order) {
        return deliveryRepository.findByOrder(order);
    }

    public long activeDeliveryCount(User driver) {
        return deliveryRepository.countByDriverAndStatusIn(driver, ACTIVE_DELIVERY_STATUSES);
    }

    public EarningsSummary getEarningsSummary(User driver) {
        BigDecimal today = deliveryRepository.sumDeliveredFeesByDriverSince(driver, LocalDate.now().atStartOfDay());
        BigDecimal total = deliveryRepository.sumDeliveredFeesByDriver(driver);
        List<Delivery> history = deliveryRepository.findByDriverAndStatusOrderByDeliveredAtDesc(
                driver,
                DeliveryStatus.DELIVERED);
        int completed = history.size();
        BigDecimal average = completed == 0
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);
        return new EarningsSummary(today, total, completed, average, history);
    }

    @Transactional
    public Delivery assignDriver(Long orderId, User driver) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.ACCEPTED && order.getStatus() != OrderStatus.PREPARING
                && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Order must be accepted before assigning delivery.");
        }
        DriverProfile profile = ensureDriverProfile(driver);
        refreshAutomaticStatus(profile);
        if (profile.getStatus() != DriverStatus.AVAILABLE) {
            throw new IllegalStateException("Driver is not available for new deliveries.");
        }
        if (activeDeliveryCount(driver) >= profile.getActiveDeliveryLimit()) {
            profile.setStatus(DriverStatus.BUSY);
            throw new IllegalStateException("Driver has reached the active delivery limit.");
        }

        order.setAssignedDriver(driver);
        Delivery delivery = deliveryRepository.findByOrder(order)
                .map(existingDelivery -> {
                    existingDelivery.setDriver(driver);
                    existingDelivery.setStatus(DeliveryStatus.ASSIGNED);
                    prepareAssignmentRoute(existingDelivery);
                    return existingDelivery;
                })
                .orElseGet(() -> {
                    Delivery newDelivery = new Delivery(order, driver);
                    prepareAssignmentRoute(newDelivery);
                    return deliveryRepository.save(newDelivery);
                });
        orderRepository.save(order);
        deliveryRepository.save(delivery);
        orderEventBroadcaster.publish(order);
        orderEventBroadcaster.publishGlobal();
        return delivery;
    }

    @Transactional
    public Delivery acceptDelivery(Long deliveryId) {
        Delivery delivery = getDelivery(deliveryId);
        requireStatus(delivery, DeliveryStatus.ASSIGNED);
        DriverProfile profile = ensureDriverProfile(delivery.getDriver());
        refreshAutomaticStatus(profile);
        if (profile.getStatus() == DriverStatus.OFFLINE) {
            throw new IllegalStateException("Driver is offline and cannot accept deliveries.");
        }
        if (activeDeliveryCount(delivery.getDriver()) >= profile.getActiveDeliveryLimit()) {
            profile.setStatus(DriverStatus.BUSY);
            throw new IllegalStateException("Driver has reached the active delivery limit.");
        }
        delivery.setStatus(DeliveryStatus.ACCEPTED_BY_DRIVER);
        refreshAutomaticStatus(profile);
        orderEventBroadcaster.publish(delivery.getOrder());
        return delivery;
    }

    @Transactional
    public Delivery rejectDelivery(Long deliveryId) {
        return declineDelivery(deliveryId);
    }

    @Transactional
    public Delivery declineDelivery(Long deliveryId) {
        Delivery delivery = getDelivery(deliveryId);
        requireStatus(delivery, DeliveryStatus.ASSIGNED);
        User declinedDriver = delivery.getDriver();
        delivery.getOrder().setAssignedDriver(null);
        delivery.setDriver(null);
        delivery.setStatus(DeliveryStatus.UNASSIGNED);
        if (declinedDriver != null) {
            refreshAutomaticStatus(ensureDriverProfile(declinedDriver));
        }
        deliveryRepository.save(delivery);
        orderEventBroadcaster.publish(delivery.getOrder());
        orderEventBroadcaster.publishGlobal();
        return delivery;
    }

    @Transactional
    public Delivery markPickedUp(Long deliveryId) {
        Delivery delivery = getDelivery(deliveryId);
        requireStatus(delivery, DeliveryStatus.ACCEPTED_BY_DRIVER);
        delivery.setStatus(DeliveryStatus.ON_THE_WAY);
        delivery.getOrder().setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(delivery.getOrder());
        deliveryRepository.save(delivery);
        orderEventBroadcaster.publish(delivery.getOrder());
        orderEventBroadcaster.publishGlobal();
        return delivery;
    }

    @Transactional
    public Delivery markOnTheWay(Long deliveryId) {
        Delivery delivery = getDelivery(deliveryId);
        if (delivery.getStatus() != DeliveryStatus.ACCEPTED_BY_DRIVER
                && delivery.getStatus() != DeliveryStatus.PICKED_UP) {
            requireStatus(delivery, DeliveryStatus.PICKED_UP);
        }
        delivery.setStatus(DeliveryStatus.ON_THE_WAY);
        delivery.getOrder().setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(delivery.getOrder());
        deliveryRepository.save(delivery);
        orderEventBroadcaster.publish(delivery.getOrder());
        orderEventBroadcaster.publishGlobal();
        return delivery;
    }

    @Transactional
    public Delivery markDelivered(Long deliveryId) {
        Delivery delivery = getDelivery(deliveryId);
        if (delivery.getProofType() == null) {
            throw new IllegalStateException("QR confirmation or photo proof is required before completing dropoff.");
        }
        completeDropoff(delivery);
        return delivery;
    }

    @Transactional
    public Delivery markDelivered(Long deliveryId, String confirmationToken, String proofImageUrl) {
        if (proofImageUrl != null && !proofImageUrl.isBlank()) {
            return completeDropoffWithPhoto(deliveryId, proofImageUrl);
        }
        return completeDropoffWithQr(deliveryId, confirmationToken);
    }

    @Transactional
    public Delivery completeDropoffWithQr(Long deliveryId, String confirmationToken) {
        Delivery delivery = getDelivery(deliveryId);
        requireStatus(delivery, DeliveryStatus.ON_THE_WAY);
        String provided = confirmationToken == null ? "" : confirmationToken.trim();
        if (!provided.equals(delivery.getConfirmationToken()) && !provided.equalsIgnoreCase(delivery.getConfirmationCode())) {
            throw new IllegalArgumentException("Delivery confirmation code does not match.");
        }
        delivery.confirmQrProof();
        completeDropoff(delivery);
        return delivery;
    }

    @Transactional
    public Delivery completeDropoffWithPhoto(Long deliveryId, String proofImageUrl) {
        Delivery delivery = getDelivery(deliveryId);
        requireStatus(delivery, DeliveryStatus.ON_THE_WAY);
        if (proofImageUrl == null || proofImageUrl.isBlank()) {
            throw new IllegalArgumentException("Proof image URL is required.");
        }
        delivery.confirmPhotoProof(proofImageUrl.trim());
        completeDropoff(delivery);
        return delivery;
    }

    private void completeDropoff(Delivery delivery) {
        requireStatus(delivery, DeliveryStatus.ON_THE_WAY);
        if (delivery.getProofType() == null) {
            throw new IllegalStateException("QR confirmation or photo proof is required before completing dropoff.");
        }
        User driver = delivery.getDriver();
        if (delivery.getDeliveryFee() == null) {
            prepareAssignmentRoute(delivery);
        }
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.getOrder().setStatus(OrderStatus.DELIVERED);
        orderRepository.save(delivery.getOrder());
        deliveryRepository.save(delivery);
        refreshAutomaticStatus(ensureDriverProfile(driver));
        orderEventBroadcaster.publish(delivery.getOrder());
        orderEventBroadcaster.publishGlobal();
    }

    private void prepareAssignmentRoute(Delivery delivery) {
        if (delivery.getConfirmationToken() == null || delivery.getConfirmationToken().isBlank()) {
            delivery.setConfirmationToken(UUID.randomUUID().toString());
        }
        RouteEstimate route = routeService.estimateRoute(restaurantPoint(delivery.getOrder()), deliveryPoint(delivery.getOrder()));
        delivery.setDistanceKm(route.distanceKm());
        delivery.setEstimatedMinutes(route.estimatedMinutes());
        delivery.setDeliveryFee(calculateDeliveryFee(route.distanceKm()));
        delivery.setRoutePreviewUrl(delivery.getOrder().getRoutePreviewUrl() == null
                ? route.previewUrl()
                : delivery.getOrder().getRoutePreviewUrl());
    }

    private Optional<GeoPoint> restaurantPoint(CustomerOrder order) {
        if (order.getRestaurant() == null) {
            return Optional.empty();
        }
        if (order.getRestaurant().getLatitude() != null && order.getRestaurant().getLongitude() != null) {
            return Optional.of(new GeoPoint(order.getRestaurant().getLatitude(), order.getRestaurant().getLongitude()));
        }
        if (order.getRestaurant().getAddress() != null && !order.getRestaurant().getAddress().isBlank()) {
            return locationService.forwardGeocode(order.getRestaurant().getAddress())
                    .map(GeocodedLocation::coordinates);
        }
        return Optional.empty();
    }

    private Optional<GeoPoint> deliveryPoint(CustomerOrder order) {
        if (order.getDeliveryAddress() == null || order.getDeliveryAddress().isBlank()) {
            return Optional.empty();
        }
        if (order.getDeliveryLatitude() != null && order.getDeliveryLongitude() != null) {
            return Optional.of(new GeoPoint(order.getDeliveryLatitude(), order.getDeliveryLongitude()));
        }
        return locationService.forwardGeocode(order.getFullDeliveryAddress()).map(GeocodedLocation::coordinates);
    }

    private BigDecimal calculateDeliveryFee(BigDecimal distanceKm) {
        BigDecimal distance = distanceKm == null ? BigDecimal.ZERO : distanceKm;
        return DELIVERY_FEE_FLAT_EUR
                .add(distance.multiply(DELIVERY_FEE_PER_KM_EUR))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private DriverProfile ensureDriverProfile(User driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver is required.");
        }
        return driverProfileRepository.findByUser(driver)
                .orElseGet(() -> driverProfileRepository.save(new DriverProfile(driver)));
    }

    private void refreshAutomaticStatus(DriverProfile profile) {
        if (profile.getStatus() == DriverStatus.OFFLINE) {
            return;
        }
        long activeCount = activeDeliveryCount(profile.getUser());
        profile.setStatus(activeCount >= profile.getActiveDeliveryLimit() ? DriverStatus.BUSY : DriverStatus.AVAILABLE);
        driverProfileRepository.save(profile);
    }

    private Delivery getDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
    }

    private void requireStatus(Delivery delivery, DeliveryStatus expected) {
        if (delivery.getStatus() != expected) {
            throw new IllegalStateException("Expected " + expected + " but was " + delivery.getStatus());
        }
    }

    public record EarningsSummary(
            BigDecimal todayEarnings,
            BigDecimal totalEarnings,
            int completedDeliveries,
            BigDecimal averageDeliveryFee,
            List<Delivery> history) {
    }
}
