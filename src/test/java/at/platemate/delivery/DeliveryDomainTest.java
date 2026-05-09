package at.platemate.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import at.platemate.order.CustomerOrder;
import at.platemate.user.Role;
import at.platemate.user.User;
import org.junit.jupiter.api.Test;

class DeliveryDomainTest {

    @Test
    void deliveryStartsAssignedWithDriverTimestampAndConfirmationCode() {
        CustomerOrder order = new CustomerOrder(null, null, java.math.BigDecimal.ZERO);
        User driver = new User("Dana Driver", Role.DRIVER);

        Delivery delivery = new Delivery(order, driver);

        assertThat(delivery.getOrder()).isSameAs(order);
        assertThat(delivery.getDriver()).isSameAs(driver);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(delivery.getAssignedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(delivery.getConfirmationToken()).isNotBlank();
        assertThat(delivery.getConfirmationCode()).hasSize(6).isUpperCase();
    }

    @Test
    void statusChangesRecordPickupOnceAndDeliveryTimeOnDelivery() {
        Delivery delivery = new Delivery(null, null);

        delivery.setStatus(DeliveryStatus.PICKED_UP);
        LocalDateTime firstPickupTime = delivery.getPickedUpAt();

        delivery.setStatus(DeliveryStatus.ON_THE_WAY);
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setStatus(DeliveryStatus.DELIVERED);

        assertThat(firstPickupTime).isNotNull();
        assertThat(delivery.getPickedUpAt()).isEqualTo(firstPickupTime);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getDeliveredAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void proofConfirmationRecordsProofTypeAndTimestamp() {
        Delivery delivery = new Delivery(null, null);

        delivery.confirmQrProof();

        assertThat(delivery.getProofType()).isEqualTo(ProofType.QR_CODE);
        assertThat(delivery.getConfirmedAt()).isBeforeOrEqualTo(LocalDateTime.now());

        delivery.confirmPhotoProof("/uploads/proof.jpg");

        assertThat(delivery.getProofType()).isEqualTo(ProofType.PHOTO);
        assertThat(delivery.getProofImageUrl()).isEqualTo("/uploads/proof.jpg");
        assertThat(delivery.getConfirmedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void setDriverRefreshesAssignmentAndRegeneratesMissingConfirmationToken() {
        Delivery delivery = new Delivery(null, null);
        delivery.setConfirmationToken(" ");
        LocalDateTime originalAssignedAt = delivery.getAssignedAt();

        delivery.setDriver(new User("New Driver", Role.DRIVER));

        assertThat(delivery.getDriver().getDisplayName()).isEqualTo("New Driver");
        assertThat(delivery.getAssignedAt()).isAfterOrEqualTo(originalAssignedAt);
        assertThat(delivery.getConfirmationToken()).isNotBlank();
    }

    @Test
    void driverProfileNormalizesNullStatusAndInvalidDeliveryLimit() {
        DriverProfile profile = new DriverProfile(new User("Driver", Role.DRIVER));

        profile.setStatus(null);
        profile.setActiveDeliveryLimit(0);

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.OFFLINE);
        assertThat(profile.getActiveDeliveryLimit()).isEqualTo(DriverProfile.DEFAULT_ACTIVE_DELIVERY_LIMIT);

        profile.setStatus(DriverStatus.AVAILABLE);
        profile.setActiveDeliveryLimit(5);

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
        assertThat(profile.getActiveDeliveryLimit()).isEqualTo(5);
    }

    @Test
    void emptyConfirmationTokenProducesEmptyCode() {
        Delivery delivery = new Delivery(null, null);

        delivery.setConfirmationToken("");

        assertThat(delivery.getConfirmationCode()).isEmpty();
    }

    @SuppressWarnings("unused")
    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not set field " + name, e);
        }
    }
}
