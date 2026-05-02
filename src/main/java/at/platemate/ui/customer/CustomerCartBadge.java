package at.platemate.ui.customer;

import at.platemate.cart.CartService;
import com.vaadin.flow.component.UI;

final class CustomerCartBadge {

    private CustomerCartBadge() {
    }

    static void update(CartService cartService) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        ui.getPage().executeJs("""
                document.querySelectorAll('.pm-cart-badge').forEach((badge) => {
                  badge.textContent = String($0);
                  badge.classList.toggle('is-empty', Number($0) === 0);
                });
                """, cartService.getTotalQuantity());
    }
}
