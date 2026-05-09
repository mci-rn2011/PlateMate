package at.platemate.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import at.platemate.order.CustomerOrder;
import at.platemate.order.OrderEventBroadcaster;
import at.platemate.order.OrderRepository;
import at.platemate.order.OrderStatus;
import at.platemate.restaurant.Restaurant;
import at.platemate.user.Role;
import at.platemate.user.User;
import at.platemate.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderEventBroadcaster orderEventBroadcaster;

    @Mock
    private LocationService locationService;

    @Mock
    private RouteService routeService;

    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(
                deliveryRepository,
                driverProfileRepository,
                orderRepository,
                userRepository,
                orderEventBroadcaster,
                locationService,
                routeService);
    }

    @Test
    void assignDriverCreatesDeliveryWithRouteAndFeeForAcceptedOrder() {
        User driver = new User("Driver", Role.DRIVER);
        DriverProfile profile = availableProfile(driver);
        CustomerOrder order = orderWithStatus(OrderStatus.ACCEPTED);
        order.getRestaurant().setCoordinates(47.27, 11.39);

        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
        when(driverProfileRepository.findByUser(driver)).thenReturn(Optional.of(profile));
        when(deliveryRepository.countByDriverAndStatusIn(any(), any())).thenReturn(0L);
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.empty());
        when(routeService.estimateRoute(any(), any()))
                .thenReturn(new RouteEstimate(new BigDecimal("10.00"), 22, "/assigned-route.svg"));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Delivery delivery = deliveryService.assignDriver(12L, driver);

        assertEquals(driver, order.getAssignedDriver());
        assertEquals(driver, delivery.getDriver());
        assertEquals(DeliveryStatus.ASSIGNED, delivery.getStatus());
        assertEquals(new BigDecimal("10.00"), delivery.getDistanceKm());
        assertEquals(22, delivery.getEstimatedMinutes());
        assertEquals(new BigDecimal("5.50"), delivery.getDeliveryFee());
        assertEquals("/assigned-route.svg", delivery.getRoutePreviewUrl());
        assertNotNull(delivery.getConfirmationToken());
        verify(orderRepository).save(order);
        verify(orderEventBroadcaster).publish(order);
        verify(orderEventBroadcaster).publishGlobal();
    }

    @Test
    void assignDriverRejectsOrderBeforeRestaurantAcceptance() {
        User driver = new User("Driver", Role.DRIVER);
        CustomerOrder order = orderWithStatus(OrderStatus.PLACED);
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deliveryService.assignDriver(12L, driver));

        assertEquals("Order must be accepted before assigning delivery.", exception.getMessage());
        verify(deliveryRepository, never()).save(any(Delivery.class));
        verify(orderEventBroadcaster, never()).publish(any(CustomerOrder.class));
    }

    @Test
    void acceptDeliveryMovesAssignedDeliveryToAcceptedByDriver() {
        User driver = new User("Driver", Role.DRIVER);
        DriverProfile profile = availableProfile(driver);
        CustomerOrder order = orderWithStatus(OrderStatus.READY_FOR_PICKUP);
        Delivery delivery = new Delivery(order, driver);

        when(deliveryRepository.findById(33L)).thenReturn(Optional.of(delivery));
        when(driverProfileRepository.findByUser(driver)).thenReturn(Optional.of(profile));
        when(deliveryRepository.countByDriverAndStatusIn(any(), any())).thenReturn(0L);

        Delivery accepted = deliveryService.acceptDelivery(33L);

        assertEquals(DeliveryStatus.ACCEPTED_BY_DRIVER, accepted.getStatus());
        verify(orderEventBroadcaster).publish(order);
    }

    @Test
    void markPickedUpRejectsDeliveryThatWasNotAcceptedByDriver() {
        User driver = new User("Driver", Role.DRIVER);
        CustomerOrder order = orderWithStatus(OrderStatus.READY_FOR_PICKUP);
        Delivery delivery = new Delivery(order, driver);
        when(deliveryRepository.findById(33L)).thenReturn(Optional.of(delivery));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deliveryService.markPickedUp(33L));

        assertEquals("Expected ACCEPTED_BY_DRIVER but was ASSIGNED", exception.getMessage());
        assertEquals(DeliveryStatus.ASSIGNED, delivery.getStatus());
        assertEquals(OrderStatus.READY_FOR_PICKUP, order.getStatus());
        verify(orderRepository, never()).save(any(CustomerOrder.class));
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    void completeDropoffWithQrDeliversOrderWhenCodeMatches() {
        User driver = new User("Driver", Role.DRIVER);
        DriverProfile profile = availableProfile(driver);
        CustomerOrder order = orderWithStatus(OrderStatus.OUT_FOR_DELIVERY);
        Delivery delivery = new Delivery(order, driver);
        delivery.setStatus(DeliveryStatus.ON_THE_WAY);
        delivery.setConfirmationToken("abcdef-123456");
        delivery.setDeliveryFee(new BigDecimal("4.25"));

        when(deliveryRepository.findById(33L)).thenReturn(Optional.of(delivery));
        when(driverProfileRepository.findByUser(driver)).thenReturn(Optional.of(profile));
        when(deliveryRepository.countByDriverAndStatusIn(any(), any())).thenReturn(0L);

        Delivery delivered = deliveryService.completeDropoffWithQr(33L, "ABCDEF");

        assertEquals(DeliveryStatus.DELIVERED, delivered.getStatus());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(ProofType.QR_CODE, delivered.getProofType());
        assertNotNull(delivered.getConfirmedAt());
        verify(orderRepository).save(order);
        verify(deliveryRepository).save(delivery);
        verify(orderEventBroadcaster).publish(order);
        verify(orderEventBroadcaster).publishGlobal();
    }

    private DriverProfile availableProfile(User driver) {
        DriverProfile profile = new DriverProfile(driver);
        profile.setStatus(DriverStatus.AVAILABLE);
        return profile;
    }

    private CustomerOrder orderWithStatus(OrderStatus status) {
        Restaurant restaurant = new Restaurant("Plate House", "Fresh meals", "Bowls", "Main Street 1", true, null);
        CustomerOrder order = new CustomerOrder(
                new User("Customer", Role.CUSTOMER),
                restaurant,
                BigDecimal.TEN,
                null,
                "Customer",
                "+43 660 123456",
                "customer@example.test",
                "Campus Alley 4",
                "6020",
                "Innsbruck",
                null);
        order.setStatus(status);
        return order;
    }
}
