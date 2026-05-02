package at.platemate.cart;

import java.math.BigDecimal;

import at.platemate.menu.MenuItem;

public class CartLine {

    private final MenuItem menuItem;
    private int quantity;

    public CartLine(MenuItem menuItem, int quantity) {
        this.menuItem = menuItem;
        this.quantity = quantity;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public void increase() {
        this.quantity++;
    }

    public void decrease() {
        if (quantity > 0) {
            this.quantity--;
        }
    }

    public BigDecimal getLineTotal() {
        return menuItem.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
