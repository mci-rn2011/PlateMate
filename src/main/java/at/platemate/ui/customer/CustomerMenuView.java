package at.platemate.ui.customer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import at.platemate.cart.CartLine;
import at.platemate.cart.CartService;
import at.platemate.menu.MenuCategory;
import at.platemate.menu.MenuItem;
import at.platemate.menu.MenuService;
import at.platemate.restaurant.Restaurant;
import at.platemate.restaurant.RestaurantEventBroadcaster;
import at.platemate.restaurant.RestaurantOpeningHours;
import at.platemate.restaurant.RestaurantService;
import at.platemate.restaurant.RestaurantStatus;
import at.platemate.ui.layout.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteParameters;

@Route(value = "customer/restaurants/:restaurantId", layout = MainLayout.class)
@RouteAlias(value = "customer/menu/:restaurantId", layout = MainLayout.class)
@PageTitle("Restaurant | PlateMate")
public class CustomerMenuView extends VerticalLayout implements BeforeEnterObserver {

    private static final String PLACEHOLDER_BANNER = "placeholders/restaurant-banner.svg";
    private static final String PLACEHOLDER_LOGO = "placeholders/restaurant-logo.svg";
    private static final String PLACEHOLDER_ITEM = "placeholders/menu-item.svg";

    private final RestaurantService restaurantService;
    private final MenuService menuService;
    private final CartService cartService;
    private final RestaurantEventBroadcaster restaurantEventBroadcaster;
    private final Div menuBoard = new Div();
    private final Div floatingCheckout = new Div();

    private Restaurant restaurant;
    private RestaurantEventBroadcaster.Registration registration;
    private boolean closedDialogShown;

    public CustomerMenuView(
            RestaurantService restaurantService,
            MenuService menuService,
            CartService cartService,
            RestaurantEventBroadcaster restaurantEventBroadcaster) {
        this.restaurantService = restaurantService;
        this.menuService = menuService;
        this.cartService = cartService;
        this.restaurantEventBroadcaster = restaurantEventBroadcaster;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames("pm-customer-page", "pm-menu-page");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Long id = Long.valueOf(event.getRouteParameters().get("restaurantId").orElseThrow());
        this.restaurant = restaurantService.getRestaurant(id, getLocale());
        render();
        if (!cartService.isEmpty() && !cartService.belongsTo(restaurant)) {
            showSwitchRestaurantDialog();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        registration = restaurantEventBroadcaster.subscribe(() -> ui.access(this::refreshRestaurantStatus));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    private void refreshRestaurantStatus() {
        if (restaurant == null || restaurant.getId() == null) {
            return;
        }
        RestaurantStatus previous = restaurant.getStatus();
        restaurant = restaurantService.getRestaurant(restaurant.getId(), getLocale());
        render();
        if (previous != RestaurantStatus.CLOSED && restaurant.getStatus() == RestaurantStatus.CLOSED && !closedDialogShown) {
            closedDialogShown = true;
            showClosedDialog();
        }
    }

    private void render() {
        removeAll();
        add(createBackLink(), createHeader(), createCategoryScroller(), menuBoard, floatingCheckout);
        refreshMenu();
    }

    private Button createBackLink() {
        Button back = new Button(getTranslation("customer.menu.backToDiscover"),
                event -> getUI().ifPresent(ui -> ui.navigate(CustomerDiscoverView.class)));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        back.addClassNames("pm-back-link", "pm-soft-action");
        return back;
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassNames("pm-customer-hero", "pm-menu-hero");

        Div visual = new Div();
        visual.addClassName("pm-menu-visual");
        Image banner = new Image(imageOrPlaceholder(restaurant.getBannerImageUrl(), PLACEHOLDER_BANNER),
                getTranslation("restaurant.studio.image.bannerAlt", restaurant.getName(getLocale())));
        banner.addClassName("pm-storefront-banner");
        Image logo = new Image(imageOrPlaceholder(restaurant.getLogoImageUrl(), PLACEHOLDER_LOGO),
                getTranslation("restaurant.studio.image.logoAlt", restaurant.getName(getLocale())));
        logo.addClassName("pm-storefront-logo");
        visual.add(banner, logo);

        Div copy = new Div();
        copy.addClassName("pm-menu-hero-copy");
        Span eyebrow = new Span(restaurant.getCategory(getLocale()));
        eyebrow.addClassName("pm-eyebrow");
        H1 title = new H1(restaurant.getName(getLocale()));
        Paragraph description = new Paragraph(restaurant.getDescription(getLocale()));
        Span address = new Span(restaurant.getAddress());
        address.addClassName("pm-muted-line");
        Span status = statusPill();
        Paragraph warning = new Paragraph(restaurant.getStatus() == RestaurantStatus.CLOSED
                ? getTranslation("customer.menu.closedWarning")
                : restaurant.getStatus() == RestaurantStatus.BUSY
                ? getTranslation("customer.menu.busyWarning")
                : getTranslation("customer.menu.openHint"));
        warning.addClassName("pm-menu-status-copy");
        copy.add(eyebrow, title, description, address, status, warning);
        header.add(visual, copy);
        return header;
    }

    private Div createCategoryScroller() {
        Div bar = new Div();
        bar.addClassName("pm-menu-category-bar");
        Button left = new Button("<", event -> scrollCategoryChips(-1));
        Button right = new Button(">", event -> scrollCategoryChips(1));
        left.addClassName("pm-chip-arrow");
        right.addClassName("pm-chip-arrow");
        Div chips = new Div();
        chips.addClassName("pm-chip-scroller");
        chips.getElement().setAttribute("id", "pm-menu-category-scroller");
        for (MenuCategory category : menuService.findCategories(restaurant, getLocale())) {
            Button chip = new Button(category.getName(getLocale()),
                    event -> getElement().executeJs("document.getElementById($0)?.scrollIntoView({ behavior: 'smooth', block: 'start' })",
                            categoryAnchor(category)));
            chip.addClassName("pm-filter-chip");
            chips.add(chip);
        }
        bar.add(left, chips, right);
        return bar;
    }

    private void scrollCategoryChips(int direction) {
        getElement().executeJs("document.getElementById('pm-menu-category-scroller')?.scrollBy({ left: $0 * 280, behavior: 'smooth' })",
                direction);
    }

    private void refreshMenu() {
        menuBoard.removeAll();
        menuBoard.addClassName("pm-customer-menu-board");
        List<MenuItem> items = menuService.findItems(restaurant, getLocale()).stream()
                .sorted(Comparator.comparingInt((MenuItem item) -> item.getCategory() == null ? Integer.MAX_VALUE : item.getCategory().getSortOrder())
                        .thenComparingInt(MenuItem::getSortOrder)
                        .thenComparing(item -> item.getName(getLocale())))
                .toList();

        for (MenuCategory category : menuService.findCategories(restaurant, getLocale())) {
            Div section = new Div();
            section.addClassName("pm-menu-section");
            section.getElement().setAttribute("id", categoryAnchor(category));
            section.add(new H2(category.getName(getLocale())));
            if (category.getDescription(getLocale()) != null && !category.getDescription(getLocale()).isBlank()) {
                section.add(new Paragraph(category.getDescription(getLocale())));
            }
            items.stream()
                    .filter(item -> item.getCategory() != null && item.getCategory().getId().equals(category.getId()))
                    .forEach(item -> section.add(createMenuItem(item)));
            menuBoard.add(section);
        }
        refreshFloatingCheckout();
    }

    private Div createMenuItem(MenuItem item) {
        Div card = new Div();
        card.addClassName("pm-customer-menu-item");
        if (!item.isAvailable() || restaurant.getStatus() == RestaurantStatus.CLOSED) {
            card.addClassName("is-muted");
        }

        Image thumb = new Image(imageOrPlaceholder(item.getThumbnailImageUrl(), PLACEHOLDER_ITEM), item.getName(getLocale()));
        thumb.addClassName("pm-item-thumb");
        Div copy = new Div();
        copy.addClassName("pm-menu-item-copy");
        copy.add(new H3(item.getName(getLocale())), new Paragraph(item.getDescription(getLocale())));
        Span price = new Span(money(item.getPrice()));
        price.addClassName("pm-price");

        Div actions = new Div();
        actions.addClassName("pm-quantity-actions");
        updateQuantityActions(actions, item);

        copy.add(price);
        card.add(thumb, copy, actions);
        return card;
    }

    private void updateQuantityActions(Div container, MenuItem item) {
        container.removeAll();
        if (restaurant.getStatus() == RestaurantStatus.CLOSED || !item.isAvailable()) {
            Button disabled = new Button(item.isAvailable()
                    ? getTranslation("customer.menu.closedButton")
                    : getTranslation("customer.menu.unavailable"));
            disabled.setEnabled(false);
            container.add(disabled);
            return;
        }

        CartLine line = cartService.findLine(item).orElse(null);
        if (line == null) {
            Button add = new Button(getTranslation("action.add"), event -> {
                try {
                    cartService.add(item);
                    CustomerCartBadge.update(cartService);
                    updateQuantityActions(container, item);
                    refreshFloatingCheckout();
                } catch (IllegalStateException exception) {
                    showSwitchRestaurantDialog();
                }
            });
            add.addClassName("pm-primary-action");
            container.add(add);
            return;
        }

        Button minus = new Button(line.getQuantity() == 1 ? VaadinIcon.TRASH.create() : VaadinIcon.MINUS.create(),
                event -> {
                    cartService.decrement(item);
                    CustomerCartBadge.update(cartService);
                    refreshMenu();
                });
        Span quantity = new Span(String.valueOf(line.getQuantity()));
        quantity.addClassName("pm-quantity-count");
        Button plus = new Button(VaadinIcon.PLUS.create(), event -> {
            cartService.increment(item);
            CustomerCartBadge.update(cartService);
            refreshMenu();
        });
        minus.addClassName("pm-quantity-button");
        plus.addClassName("pm-quantity-button");
        container.add(minus, quantity, plus);
    }

    private void showSwitchRestaurantDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("customer.cart.switch.title"));
        dialog.add(new Paragraph(getTranslation("customer.cart.switch.detail", restaurant.getName(getLocale()))));
        Button discover = new Button(getTranslation("customer.cart.switch.discovery"), event -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.navigate(CustomerDiscoverView.class));
        });
        Button clear = new Button(getTranslation("customer.cart.switch.clear"), event -> {
            cartService.clear();
            CustomerCartBadge.update(cartService);
            dialog.close();
            refreshMenu();
        });
        clear.addClassName("pm-primary-action");
        dialog.addDialogCloseActionListener(event -> getUI().ifPresent(ui -> ui.navigate(CustomerDiscoverView.class)));
        dialog.getFooter().add(discover, clear);
        dialog.open();
    }

    private void showClosedDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("customer.menu.closedDialog.title"));
        dialog.add(new Paragraph(getTranslation("customer.menu.closedDialog.detail", nextOpeningText())));
        Button discover = new Button(getTranslation("customer.cart.switch.discovery"), event -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.navigate(CustomerDiscoverView.class));
        });
        Button stay = new Button(getTranslation("action.close"), event -> dialog.close());
        discover.addClassName("pm-primary-action");
        dialog.getFooter().add(stay, discover);
        dialog.open();
    }

    private void refreshFloatingCheckout() {
        floatingCheckout.removeAll();
        floatingCheckout.addClassName("pm-floating-checkout");
        boolean visible = !cartService.isEmpty() && cartService.belongsTo(restaurant);
        floatingCheckout.setVisible(visible);
        if (!visible) {
            return;
        }
        Span summary = new Span(getTranslation("customer.menu.floatingCart",
                cartService.getTotalQuantity(), money(cartService.getTotal())));
        summary.addClassName("pm-floating-checkout-summary");
        Button checkout = new Button(getTranslation("customer.menu.checkout"),
                event -> getUI().ifPresent(ui -> ui.navigate(CustomerCartView.class)));
        checkout.addClassName("pm-primary-action");
        floatingCheckout.add(summary, checkout);
    }

    private Span statusPill() {
        Span status = new Span(getTranslation("restaurantStatus." + restaurant.getStatus().name()));
        status.addClassNames("pm-status-pill", switch (restaurant.getStatus()) {
            case OPEN -> "is-open";
            case BUSY -> "is-busy";
            case CLOSED -> "is-closed";
        });
        return status;
    }

    private String nextOpeningText() {
        List<RestaurantOpeningHours> hours = restaurantService.findOpeningHours(restaurant);
        java.time.DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        java.time.LocalTime now = java.time.LocalTime.now();
        java.time.format.DateTimeFormatter timeFormat = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        for (int offset = 0; offset < 8; offset++) {
            int dayOffset = offset;
            java.time.DayOfWeek day = today.plus(offset);
            java.util.Optional<RestaurantOpeningHours> match = hours.stream()
                    .filter(row -> row.getDayOfWeek() == day)
                    .filter(row -> !row.isClosed())
                    .filter(row -> row.getOpensAt() != null)
                    .filter(row -> dayOffset > 0 || row.getOpensAt().isAfter(now))
                    .findFirst();
            if (match.isPresent()) {
                String label = offset == 0
                        ? getTranslation("customer.discover.today")
                        : offset == 1
                        ? getTranslation("customer.discover.tomorrow")
                        : getTranslation("dayOfWeek." + day.name());
                return getTranslation("customer.discover.opensAgain", label, match.get().getOpensAt().format(timeFormat));
            }
        }
        return getTranslation("customer.discover.closed");
    }

    private String categoryAnchor(MenuCategory category) {
        return "category-" + category.getId();
    }

    private String money(Object amount) {
        return amount + " €";
    }

    private String imageOrPlaceholder(String imageUrl, String placeholder) {
        return imageUrl == null || imageUrl.isBlank() ? placeholder : imageUrl;
    }
}
