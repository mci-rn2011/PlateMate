package at.platemate.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import at.platemate.menu.MenuItem;
import at.platemate.restaurant.Restaurant;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Service;

@Service
@VaadinSessionScope
public class CartService {

    private final Map<Long, CartLine> lines = new LinkedHashMap<>();
    private Long restaurantId;

    public void add(MenuItem item) {
        requireMenuItem(item);
        Long itemRestaurantId = item.getRestaurant().getId();
        if (requiresRestaurantSwitch(item)) {
            throw new IllegalStateException("Cart already contains items from another restaurant.");
        }
        restaurantId = itemRestaurantId;
        lines.compute(item.getId(), (id, existing) -> {
            if (existing == null) {
                return new CartLine(item, 1);
            }
            existing.increase();
            return existing;
        });
    }

    public void increment(Long menuItemId) {
        CartLine line = lines.get(menuItemId);
        if (line != null) {
            line.increase();
        }
    }

    public void increment(MenuItem item) {
        add(item);
    }

    public void decrement(Long menuItemId) {
        CartLine line = lines.get(menuItemId);
        if (line == null) {
            return;
        }
        line.decrease();
        if (line.getQuantity() <= 0) {
            remove(menuItemId);
        }
    }

    public void decrement(MenuItem item) {
        decrement(item.getId());
    }

    public void remove(Long menuItemId) {
        lines.remove(menuItemId);
        if (lines.isEmpty()) {
            restaurantId = null;
        }
    }

    public void remove(MenuItem item) {
        remove(item.getId());
    }

    public Optional<CartLine> findLine(MenuItem item) {
        return Optional.ofNullable(lines.get(item.getId()));
    }

    public List<CartLine> getLines() {
        return new ArrayList<>(lines.values());
    }

    public Optional<Long> getRestaurantId() {
        return Optional.ofNullable(restaurantId);
    }

    public BigDecimal getTotal() {
        return lines.values().stream()
                .map(CartLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getTotalQuantity() {
        return lines.values().stream()
                .mapToInt(CartLine::getQuantity)
                .sum();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public boolean belongsTo(Restaurant restaurant) {
        return restaurant != null && belongsToRestaurantId(restaurant.getId());
    }

    public boolean canAdd(MenuItem item) {
        requireMenuItem(item);
        return belongsToRestaurantId(item.getRestaurant().getId());
    }

    public boolean requiresRestaurantSwitch(MenuItem item) {
        return !canAdd(item);
    }

    public boolean belongsToRestaurantId(Long itemRestaurantId) {
        return restaurantId == null || restaurantId.equals(itemRestaurantId);
    }

    public void clear() {
        lines.clear();
        restaurantId = null;
    }

    private void requireMenuItem(MenuItem item) {
        if (item == null || item.getRestaurant() == null || item.getRestaurant().getId() == null) {
            throw new IllegalArgumentException("Menu item must belong to a saved restaurant.");
        }
    }
}
