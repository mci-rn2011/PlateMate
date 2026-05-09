package at.platemate.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import at.platemate.restaurant.Restaurant;
import at.platemate.user.Role;
import at.platemate.user.User;
import org.junit.jupiter.api.Test;

class CustomerOrderTest {

    @Test
    void constructorSetsInitialStateAndTrimsOptionalContactDetails() {
        User customer = new User("Alex Customer", Role.CUSTOMER);
        Restaurant restaurant = new Restaurant("Bistro", "Fresh food", "Italian", "Main Street 1", true, null);

        CustomerOrder order = new CustomerOrder(
                customer,
                restaurant,
                new BigDecimal("24.80"),
                " guest-123 ",
                " Alex ",
                " +43 123 ",
                " alex@example.test ",
                " Ringstrasse 10 ",
                " 1010 ",
                " Vienna ",
                " Leave at reception ");

        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getRestaurant()).isSameAs(restaurant);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("24.80");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(order.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(order.getGuestSessionId()).isEqualTo("guest-123");
        assertThat(order.getContactName()).isEqualTo("Alex");
        assertThat(order.getContactPhone()).isEqualTo("+43 123");
        assertThat(order.getContactEmail()).isEqualTo("alex@example.test");
        assertThat(order.getDeliveryAddress()).isEqualTo("Ringstrasse 10");
        assertThat(order.getDeliveryPostalCode()).isEqualTo("1010");
        assertThat(order.getDeliveryCity()).isEqualTo("Vienna");
        assertThat(order.getDeliveryInstructions()).isEqualTo("Leave at reception");
    }

    @Test
    void blankContactAndDeliveryValuesBecomeNullAndFullAddressSkipsMissingParts() {
        CustomerOrder order = new CustomerOrder(
                null,
                null,
                BigDecimal.ZERO,
                " ",
                "",
                null,
                " ",
                " Mariahilfer Strasse 1 ",
                " ",
                " Vienna ",
                "");

        assertThat(order.getGuestSessionId()).isNull();
        assertThat(order.getContactName()).isNull();
        assertThat(order.getContactPhone()).isNull();
        assertThat(order.getContactEmail()).isNull();
        assertThat(order.getDeliveryInstructions()).isNull();
        assertThat(order.getFullDeliveryAddress()).isEqualTo("Mariahilfer Strasse 1, Vienna");

        order.setDeliveryAddressNormalized("  normalized address  ");
        order.setRoutePreviewUrl("  https://maps.example/route  ");

        assertThat(order.getDeliveryAddressNormalized()).isEqualTo("normalized address");
        assertThat(order.getRoutePreviewUrl()).isEqualTo("https://maps.example/route");
    }

    @Test
    void addItemStoresItemAndSetsBackReference() {
        CustomerOrder order = new CustomerOrder(null, null, new BigDecimal("18.00"));
        OrderItem item = new OrderItem(42L, "Noodles", 3, new BigDecimal("6.00"));

        order.addItem(item);

        assertThat(order.getItems()).containsExactly(item);
        assertThat(item.getLineTotal()).isEqualByComparingTo("18.00");
        assertThat(readOrder(item)).isSameAs(order);
    }

    private static CustomerOrder readOrder(OrderItem item) {
        try {
            Field field = OrderItem.class.getDeclaredField("order");
            field.setAccessible(true);
            return (CustomerOrder) field.get(item);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not read order back-reference", e);
        }
    }
}
