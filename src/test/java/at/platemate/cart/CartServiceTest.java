package at.platemate.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import at.platemate.menu.MenuItem;
import at.platemate.restaurant.Restaurant;
import org.junit.jupiter.api.Test;

class CartServiceTest {

    private final CartService cartService = new CartService();

    @Test
    void addCombinesSameMenuItemAndCalculatesTotals() {
        Restaurant restaurant = restaurant(1L);
        MenuItem pizza = menuItem(10L, restaurant, "Pizza", "12.50");

        cartService.add(pizza);
        cartService.add(pizza);
        cartService.increment(pizza.getId());

        assertThat(cartService.getLines()).hasSize(1);
        assertThat(cartService.findLine(pizza)).isPresent()
                .get()
                .extracting(CartLine::getQuantity)
                .isEqualTo(3);
        assertThat(cartService.getTotalQuantity()).isEqualTo(3);
        assertThat(cartService.getTotal()).isEqualByComparingTo("37.50");
        assertThat(cartService.getRestaurantId()).contains(1L);
    }

    @Test
    void addRejectsItemsFromAnotherRestaurantUntilCartIsCleared() {
        MenuItem sushi = menuItem(20L, restaurant(1L), "Sushi", "9.90");
        MenuItem burger = menuItem(30L, restaurant(2L), "Burger", "11.00");

        cartService.add(sushi);

        assertThat(cartService.canAdd(burger)).isFalse();
        assertThat(cartService.requiresRestaurantSwitch(burger)).isTrue();
        assertThatThrownBy(() -> cartService.add(burger))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another restaurant");

        cartService.clear();

        assertThat(cartService.canAdd(burger)).isTrue();
        cartService.add(burger);
        assertThat(cartService.getRestaurantId()).contains(2L);
    }

    @Test
    void decrementRemovesLastQuantityAndResetsRestaurantWhenEmpty() {
        MenuItem pasta = menuItem(40L, restaurant(7L), "Pasta", "8.00");
        cartService.add(pasta);
        cartService.add(pasta);

        cartService.decrement(pasta.getId());
        cartService.decrement(pasta.getId());

        assertThat(cartService.isEmpty()).isTrue();
        assertThat(cartService.getTotalQuantity()).isZero();
        assertThat(cartService.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cartService.getRestaurantId()).isEmpty();
    }

    @Test
    void addRequiresSavedRestaurantOnMenuItem() {
        MenuItem unsavedRestaurantItem = new MenuItem(
                new Restaurant("Unsaved", "Description", "Cafe", "Address", true, null),
                "Coffee",
                "Fresh",
                new BigDecimal("3.50"),
                true);

        assertThatThrownBy(() -> cartService.add(unsavedRestaurantItem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saved restaurant");
    }

    private static Restaurant restaurant(Long id) {
        Restaurant restaurant = new Restaurant("Restaurant " + id, "Description", "Category", "Address", true, null);
        setId(restaurant, id);
        return restaurant;
    }

    private static MenuItem menuItem(Long id, Restaurant restaurant, String name, String price) {
        MenuItem item = new MenuItem(restaurant, name, name + " description", new BigDecimal(price), true);
        setId(item, id);
        return item;
    }

    private static void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not set generated id for test fixture", e);
        }
    }
}
