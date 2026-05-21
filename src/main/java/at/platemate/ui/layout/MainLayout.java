package at.platemate.ui.layout;

import at.platemate.auth.MockSessionService;
import at.platemate.cart.CartService;
import at.platemate.order.CustomerOrder;
import at.platemate.order.OrderEventBroadcaster;
import at.platemate.order.OrderService;
import at.platemate.order.OrderStatus;
import at.platemate.user.Role;
import at.platemate.user.User;
import at.platemate.ui.customer.CustomerCartView;
import at.platemate.ui.customer.CustomerDiscoverView;
import at.platemate.ui.customer.CustomerProfileView;
import at.platemate.ui.driver.DriverDashboardView;
import at.platemate.ui.driver.DriverProfileView;
import at.platemate.ui.login.LoginView;
import at.platemate.ui.preferences.PreferenceControls;
import at.platemate.ui.preferences.UiPreferencesService;
import at.platemate.ui.restaurant.RestaurantDashboardView;
import at.platemate.ui.restaurant.RestaurantStudioView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;

import java.util.List;

public class MainLayout extends AppLayout {

    private final MockSessionService sessionService;
    private final OrderService orderService;
    private final OrderEventBroadcaster orderEventBroadcaster;
    private Div activeDeliveryFloating;
    private OrderEventBroadcaster.Registration orderRegistration;

    public MainLayout(
            MockSessionService sessionService,
            UiPreferencesService preferences,
            CartService cartService,
            OrderService orderService,
            OrderEventBroadcaster orderEventBroadcaster) {
        this.sessionService = sessionService;
        this.orderService = orderService;
        this.orderEventBroadcaster = orderEventBroadcaster;
        preferences.apply(UI.getCurrent());

        HorizontalLayout nav = new HorizontalLayout();
        nav.setAlignItems(FlexComponent.Alignment.CENTER);
        nav.setWidthFull();
        nav.setPadding(true);
        nav.setSpacing(true);
        nav.addClassName("pm-navbar");

        nav.add(createBrand());
        sessionService.getCurrentUser().ifPresent(user -> addRoleLinks(nav, user, cartService));
        Span spacer = new Span();
        nav.add(spacer);
        nav.expand(spacer);

        HorizontalLayout controls = new HorizontalLayout();
        controls.addClassName("pm-navbar-controls");
        controls.setAlignItems(FlexComponent.Alignment.CENTER);
        controls.setSpacing(true);
        sessionService.getCurrentUser().ifPresent(user -> {
            if (user.getRole() == Role.CUSTOMER) {
                controls.add(createProfileLink(user), createCartButton(cartService));
            } else if (user.getRole() == Role.DRIVER) {
                controls.add(createDriverProfileLink(user));
            } else {
                Span currentUser = new Span(user.getDisplayName());
                currentUser.addClassName("pm-current-user");
                controls.add(currentUser);
            }
        });
        controls.add(new PreferenceControls(this, preferences));
        Button logout = new Button(getTranslation("nav.logout"), event -> {
            sessionService.logout();
            UI.getCurrent().navigate(LoginView.class);
        });
        logout.setIcon(VaadinIcon.SIGN_OUT.create());
        logout.setIconAfterText(true);
        controls.add(logout);
        nav.add(controls);

        addToNavbar(nav);
        refreshActiveDeliveryShortcut();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        orderRegistration = orderEventBroadcaster.subscribeToAll(() -> ui.access(this::refreshActiveDeliveryShortcut));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (orderRegistration != null) {
            orderRegistration.unregister();
            orderRegistration = null;
        }
    }

    private HorizontalLayout createBrand() {
        Image wordmark = new Image("brand/platemate-logo-wordmark.png", "PlateMate");
        wordmark.addClassName("pm-brand-wordmark");

        HorizontalLayout brand = new HorizontalLayout(wordmark);
        brand.addClassName("pm-brand");
        brand.setAlignItems(FlexComponent.Alignment.CENTER);
        brand.setSpacing(false);
        return brand;
    }

    private void addRoleLinks(HorizontalLayout nav, User user, CartService cartService) {
        if (user.getRole() == Role.CUSTOMER) {
            nav.add(new RouterLink(getTranslation("nav.customer.discover"), CustomerDiscoverView.class));
        }
        if (user.getRole() == Role.RESTAURANT) {
            nav.add(new RouterLink(getTranslation("nav.restaurant.orders"), RestaurantDashboardView.class));
            nav.add(new RouterLink(getTranslation("nav.restaurant.menu"), RestaurantStudioView.class));
        }
        if (user.getRole() == Role.DRIVER) {
            nav.add(new RouterLink(getTranslation("nav.driver.dashboard"), DriverDashboardView.class));
            nav.add(new RouterLink(getTranslation("nav.driver.profile"), DriverProfileView.class));
        }
    }

    private RouterLink createProfileLink(User user) {
        RouterLink currentUser = new RouterLink(user.getDisplayName(), CustomerProfileView.class);
        currentUser.addClassNames("pm-current-user", "pm-current-user-link");
        return currentUser;
    }

    private RouterLink createDriverProfileLink(User user) {
        RouterLink currentUser = new RouterLink(user.getDisplayName(), DriverProfileView.class);
        currentUser.addClassNames("pm-current-user", "pm-current-user-link");
        return currentUser;
    }

    private Button createCartButton(CartService cartService) {
        Button cart = new Button();
        cart.addClickListener(event -> UI.getCurrent().navigate(CustomerCartView.class));
        cart.setIcon(VaadinIcon.CART.create());
        cart.addClassName("pm-cart-nav-button");
        cart.getElement().setAttribute("aria-label", getTranslation("nav.customer.cartLabel"));

        Span badge = new Span(String.valueOf(cartService.getTotalQuantity()));
        badge.addClassName("pm-cart-badge");
        if (cartService.getTotalQuantity() == 0) {
            badge.addClassName("is-empty");
        }
        cart.getElement().appendChild(badge.getElement());
        return cart;
    }

    private void refreshActiveDeliveryShortcut() {
        if (activeDeliveryFloating != null) {
            getElement().removeChild(activeDeliveryFloating.getElement());
            activeDeliveryFloating = null;
        }

        // Disabled for the demo presentation; active deliveries remain visible on the profile page.
        boolean showActiveDeliveryShortcut = false;
        if (!showActiveDeliveryShortcut) {
            return;
        }

        boolean hasActiveOrder = sessionService.getCurrentUser()
                .filter(user -> user.getRole() == Role.CUSTOMER)
                .map(orderService::findCustomerOrders)
                .orElseGet(() -> sessionService.isGuest()
                        ? orderService.findGuestSessionOrders(sessionService.getGuestSessionId())
                        : List.of())
                .stream()
                .anyMatch(this::isActiveCustomerOrder);
        if (!hasActiveOrder) {
            return;
        }

        Div floating = new Div();
        floating.addClassName("pm-active-delivery-floating");
        floating.add(new Span("🛵"), new Span(getTranslation("customer.profile.activeFloating")));
        Button goToProfile = new Button(getTranslation("customer.profile.goToActive"),
                event -> UI.getCurrent().navigate(CustomerProfileView.class));
        goToProfile.addClassName("pm-primary-action");
        floating.add(goToProfile);
        activeDeliveryFloating = floating;
        getElement().appendChild(floating.getElement());
    }

    private boolean isActiveCustomerOrder(CustomerOrder order) {
        return order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.REJECTED;
    }
}
