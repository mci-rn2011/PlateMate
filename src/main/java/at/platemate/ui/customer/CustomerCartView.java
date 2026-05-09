package at.platemate.ui.customer;

import java.util.List;

import at.platemate.auth.MockSessionService;
import at.platemate.cart.CartLine;
import at.platemate.cart.CartService;
import at.platemate.delivery.GeocodedLocation;
import at.platemate.delivery.LocationService;
import at.platemate.menu.MenuItem;
import at.platemate.order.OrderService;
import at.platemate.restaurant.Restaurant;
import at.platemate.restaurant.RestaurantService;
import at.platemate.ui.layout.MainLayout;
import at.platemate.user.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "customer/cart", layout = MainLayout.class)
@PageTitle("Cart | PlateMate")
public class CustomerCartView extends VerticalLayout {

    private static final String PLACEHOLDER_ITEM = "placeholders/menu-item.svg";

    private final CartService cartService;
    private final RestaurantService restaurantService;
    private final OrderService orderService;
    private final MockSessionService sessionService;
    private final LocationService locationService;
    private final Div lines = new Div();
    private final Span total = new Span();
    private final TextField name = new TextField();
    private final TextField phone = new TextField();
    private final EmailField email = new EmailField();
    private final TextField address = new TextField();
    private final TextField postalCode = new TextField();
    private final TextField city = new TextField();
    private final TextArea note = new TextArea();

    public CustomerCartView(
            CartService cartService,
            RestaurantService restaurantService,
            OrderService orderService,
            MockSessionService sessionService,
            LocationService locationService) {
        this.cartService = cartService;
        this.restaurantService = restaurantService;
        this.orderService = orderService;
        this.sessionService = sessionService;
        this.locationService = locationService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames("pm-customer-page", "pm-cart-page");
        render();
    }

    private void render() {
        removeAll();
        add(createBackLink());
        Div hero = new Div();
        hero.addClassNames("pm-customer-hero", "pm-cart-hero");
        hero.add(new H1(getTranslation("customer.cart.title")),
                new Paragraph(getTranslation("customer.cart.intro")));
        add(hero);

        if (cartService.isEmpty()) {
            add(emptyState());
            return;
        }

        prefillCheckout();
        Div layout = new Div();
        layout.addClassName("pm-cart-layout");
        lines.addClassName("pm-cart-lines");
        Div summary = createSummary();
        layout.add(lines, summary);
        add(layout);
        refreshLines();
    }

    private void refreshLines() {
        lines.removeAll();
        cartService.getRestaurantId()
                .flatMap(id -> {
                    try {
                        return java.util.Optional.of(restaurantService.getRestaurant(id, getLocale()));
                    } catch (IllegalArgumentException exception) {
                        return java.util.Optional.empty();
                    }
                })
                .ifPresent(restaurant -> lines.add(new H2(restaurant.getName(getLocale()))));

        for (CartLine line : cartService.getLines()) {
            lines.add(createLine(line));
        }
        total.setText(money(cartService.getTotal()));
        if (cartService.isEmpty()) {
            render();
        }
    }

    private Div createLine(CartLine line) {
        MenuItem item = line.getMenuItem();
        Div row = new Div();
        row.addClassName("pm-cart-line");
        Image thumb = new Image(imageOrPlaceholder(item.getThumbnailImageUrl(), PLACEHOLDER_ITEM), item.getName(getLocale()));
        thumb.addClassName("pm-cart-thumb");
        Div copy = new Div();
        copy.addClassName("pm-cart-line-copy");
        copy.add(new H3(item.getName(getLocale())), new Paragraph(item.getDescription(getLocale())),
                new Span(money(item.getPrice()) + " × " + line.getQuantity()));
        Div actions = new Div();
        actions.addClassName("pm-quantity-actions");
        Button minus = new Button(line.getQuantity() == 1 ? VaadinIcon.TRASH.create() : VaadinIcon.MINUS.create(),
                event -> {
                    cartService.decrement(item);
                    CustomerCartBadge.update(cartService);
                    refreshLines();
                });
        Span quantity = new Span(String.valueOf(line.getQuantity()));
        quantity.addClassName("pm-quantity-count");
        Button plus = new Button(VaadinIcon.PLUS.create(), event -> {
            cartService.increment(item);
            CustomerCartBadge.update(cartService);
            refreshLines();
        });
        actions.add(minus, quantity, plus);
        Span lineTotal = new Span(money(line.getLineTotal()));
        lineTotal.addClassName("pm-price");
        row.add(thumb, copy, actions, lineTotal);
        return row;
    }

    private Div createSummary() {
        Div summary = new Div();
        summary.addClassName("pm-checkout-summary");
        summary.add(new H2(getTranslation("customer.checkout.title")));

        name.setLabel(getTranslation("customer.checkout.name"));
        phone.setLabel(getTranslation("customer.checkout.phone"));
        email.setLabel(getTranslation("customer.checkout.email"));
        address.setLabel(getTranslation("customer.checkout.address"));
        postalCode.setLabel(getTranslation("customer.checkout.postalCode"));
        city.setLabel(getTranslation("customer.checkout.city"));
        note.setLabel(getTranslation("customer.checkout.note"));
        name.setWidthFull();
        phone.setWidthFull();
        email.setWidthFull();
        address.setWidthFull();
        postalCode.setWidthFull();
        city.setWidthFull();
        note.setWidthFull();
        name.setRequiredIndicatorVisible(true);
        address.setRequiredIndicatorVisible(true);
        postalCode.setRequiredIndicatorVisible(true);
        city.setRequiredIndicatorVisible(true);
        name.addClassName("pm-required-field");
        address.addClassName("pm-required-field");
        postalCode.addClassName("pm-required-field");
        city.addClassName("pm-required-field");

        Div cityGrid = new Div(postalCode, city);
        cityGrid.addClassName("pm-checkout-city-grid");

        Div totalRow = new Div(new Span(getTranslation("customer.checkout.total")), total);
        totalRow.addClassName("pm-total-row");

        Button pay = new Button(getTranslation("customer.checkout.pay"), event -> placeOrder());
        pay.addClassName("pm-primary-action");
        summary.add(name, phone, email, address, cityGrid, note, totalRow, pay);
        return summary;
    }

    private void prefillCheckout() {
        sessionService.getCurrentUser().ifPresent(user -> {
            if (name.isEmpty()) {
                name.setValue(user.getDisplayName());
            }
            if (email.isEmpty() && user.getUsername() != null && user.getUsername().contains("@")) {
                email.setValue(user.getUsername());
            }
            if (address.isEmpty() && user.getAddress() != null) {
                address.setValue(user.getAddress());
            }
            if (postalCode.isEmpty() && user.getPostalCode() != null) {
                postalCode.setValue(user.getPostalCode());
            }
            if (city.isEmpty() && user.getCity() != null) {
                city.setValue(user.getCity());
            }
        });
        sessionService.getSelectedDeliveryLocation().ifPresent(selected -> {
            if (address.isEmpty()) {
                address.setValue(selected.normalizedAddress());
            }
        });
    }

    private void placeOrder() {
        if (name.isEmpty() || address.isEmpty() || postalCode.isEmpty() || city.isEmpty()
                || (phone.isEmpty() && email.isEmpty())) {
            Notification.show(getTranslation("customer.checkout.required"));
            return;
        }
        List<GeocodedLocation> suggestions = locationService.searchForwardGeocode(buildDeliveryAddressSearchString(), 5);
        if (suggestions.isEmpty()) {
            Notification.show(getTranslation("customer.checkout.location.notFound"));
            return;
        }
        if (suggestions.size() == 1) {
            submitOrder(suggestions.get(0));
            return;
        }
        showCheckoutLocationPicker(suggestions);
    }

    private void showCheckoutLocationPicker(List<GeocodedLocation> suggestions) {
        Dialog dialog = new Dialog();
        dialog.addClassName("pm-location-picker-dialog");
        dialog.setHeaderTitle(getTranslation("customer.checkout.location.choose"));
        Div list = new Div();
        list.addClassName("pm-location-suggestion-list");
        suggestions.forEach(suggestion -> {
            Button option = new Button(suggestion.normalizedAddress(), event -> {
                dialog.close();
                submitOrder(suggestion);
            });
            option.addClassName("pm-location-suggestion");
            list.add(option);
        });
        dialog.add(list);
        dialog.getFooter().add(new Button(getTranslation("action.close"), event -> dialog.close()));
        dialog.open();
    }

    private void submitOrder(GeocodedLocation selectedLocation) {
        sessionService.setSelectedDeliveryLocation(selectedLocation);
        address.setValue(selectedLocation.normalizedAddress());
        OrderService.CheckoutDetails details = new OrderService.CheckoutDetails(
                name.getValue(),
                phone.getValue(),
                email.getValue(),
                address.getValue(),
                postalCode.getValue(),
                city.getValue(),
                note.getValue());
        User user = sessionService.getCurrentUser().orElse(null);
        if (sessionService.isGuest()) {
            orderService.placeGuestOrder(user, sessionService.getGuestSessionId(), cartService, true, details);
        } else {
            orderService.placeOrder(user, cartService, true, details);
        }
        Notification.show(getTranslation("checkout.orderPlaced"));
        CustomerCartBadge.update(cartService);
        getUI().ifPresent(ui -> ui.navigate(CustomerProfileView.class));
    }

    private String buildDeliveryAddressSearchString() {
        return String.join(", ", java.util.stream.Stream.of(address.getValue(), postalCode.getValue(), city.getValue())
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    private Div emptyState() {
        Div empty = new Div();
        empty.addClassName("pm-empty-state");
        Button discover = new Button(getTranslation("customer.cart.empty.action"),
                event -> getUI().ifPresent(ui -> ui.navigate(CustomerDiscoverView.class)));
        discover.addClassName("pm-primary-action");
        empty.add(new H2(getTranslation("customer.cart.empty.title")),
                new Paragraph(getTranslation("customer.cart.empty.detail")), discover);
        return empty;
    }

    private Button createBackLink() {
        Button continueShopping = new Button(getTranslation("customer.cart.continueShopping"),
                event -> navigateBackToShop());
        continueShopping.setIcon(VaadinIcon.ARROW_LEFT.create());
        continueShopping.addClassNames("pm-back-link", "pm-soft-action");
        return continueShopping;
    }

    private String money(Object amount) {
        return amount + " €";
    }

    private void navigateBackToShop() {
        cartService.getRestaurantId().ifPresentOrElse(
                id -> getUI().ifPresent(ui -> ui.navigate(CustomerMenuView.class,
                        new com.vaadin.flow.router.RouteParameters("restaurantId", id.toString()))),
                () -> getUI().ifPresent(ui -> ui.navigate(CustomerDiscoverView.class)));
    }

    private String imageOrPlaceholder(String imageUrl, String placeholder) {
        return imageUrl == null || imageUrl.isBlank() ? placeholder : imageUrl;
    }
}
