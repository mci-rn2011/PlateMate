package at.platemate.ui.login;

import java.util.List;
import java.util.Comparator;

import at.platemate.auth.MockSessionService;
import at.platemate.ui.customer.CustomerDiscoverView;
import at.platemate.ui.driver.DriverDashboardView;
import at.platemate.ui.legal.ImprintView;
import at.platemate.ui.legal.PrivacyView;
import at.platemate.ui.preferences.PreferenceControls;
import at.platemate.ui.preferences.UiPreferencesService;
import at.platemate.ui.restaurant.RestaurantDashboardView;
import at.platemate.user.Role;
import at.platemate.user.User;
import at.platemate.user.UserRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("")
@PageTitle("Login | PlateMate")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final List<String> FLOATERS = List.of(
            "🍔", "🍟", "🛵", "🍕", "🥤", "🌮", "🍜", "🛒",
            "🥑", "🥐", "🍩", "🥨", "🍣", "🥗", "🍝", "🍦");

    private final UserRepository userRepository;
    private final MockSessionService sessionService;
    private final UiPreferencesService preferences;

    public LoginView(
            UserRepository userRepository,
            MockSessionService sessionService,
            UiPreferencesService preferences) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.preferences = preferences;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        preferences.apply(UI.getCurrent());
        boolean adminMode = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .getOrDefault("mode", List.of())
                .contains("admin");
        render(adminMode);
    }

    private void render(boolean adminMode) {
        removeAll();
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("pm-login-view");

        add(createFloaters());
        add(createLoginCard(adminMode));
        if (adminMode) {
            add(createAdminTaskbar());
        }
    }

    private Div createLoginCard(boolean adminMode) {
        Div card = new Div();
        card.addClassName("pm-login-card");

        Image logo = new Image("brand/platemate-logo-wordmark.png", "PlateMate");
        logo.addClassName("pm-login-logo");

        Paragraph subtitle = new Paragraph(getTranslation("login.subtitle"));
        subtitle.addClassName("pm-login-subtitle");

        TextField username = new TextField(getTranslation("login.username"));
        username.setPlaceholder(getTranslation("login.username.placeholder"));
        username.setValueChangeMode(ValueChangeMode.EAGER);
        username.setWidthFull();

        PasswordField password = new PasswordField(getTranslation("login.password"));
        password.setPlaceholder(getTranslation("login.password.placeholder"));
        password.setValueChangeMode(ValueChangeMode.EAGER);
        password.setWidthFull();

        Button login = new Button(getTranslation("login.submit"), event -> {
            if (sessionService.login(username.getValue(), password.getValue())) {
                sessionService.getCurrentUser().ifPresent(user -> navigateByRole(user.getRole()));
                return;
            }
            Notification.show(getTranslation("login.invalid"));
        });
        login.addClassName("pm-login-primary");
        login.setWidthFull();

        Button guest = new Button(getTranslation("login.guest"), event -> {
            User guestUser = sessionService.loginAsGuest();
            navigateByRole(guestUser.getRole());
        });
        guest.addClassName("pm-login-guest");

        Span hint = new Span(adminMode ? getTranslation("login.adminModeActive") : getTranslation("login.demoHint"));
        hint.addClassName("pm-login-hint");

        card.add(
                logo,
                subtitle,
                new PreferenceControls(this, preferences),
                username,
                password,
                login,
                guest,
                hint,
                createFooter());
        return card;
    }

    private Div createAdminTaskbar() {
        Div taskbar = new Div();
        taskbar.addClassName("pm-admin-taskbar");

        taskbar.add(
                createAdminGroup("People", Role.CUSTOMER),
                createAdminGroup("Drivers", Role.DRIVER),
                createAdminGroup("Restaurants", Role.RESTAURANT));
        return taskbar;
    }

    private Div createAdminGroup(String title, Role role) {
        Div group = new Div();
        group.addClassNames("pm-admin-group", "is-" + role.name().toLowerCase());
        Span heading = new Span(title);
        heading.addClassName("pm-admin-group-title");

        Div shortcuts = new Div();
        shortcuts.addClassName("pm-admin-shortcut-list");
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == role)
                .filter(user -> !"guest".equalsIgnoreCase(user.getUsername()))
                .sorted(Comparator.comparing(this::firstName, String.CASE_INSENSITIVE_ORDER))
                .forEach(user -> shortcuts.add(createAdminShortcut(user)));

        group.add(heading, shortcuts);
        return group;
    }

    private Button createAdminShortcut(User user) {
        Button shortcut = new Button(roleIcon(user.getRole()) + " " + firstName(user), event -> {
            sessionService.login(user);
            navigateByRole(user.getRole());
        });
        shortcut.addClassName("pm-admin-shortcut");
        return shortcut;
    }

    private Div createFooter() {
        Div footer = new Div();
        footer.addClassName("pm-login-footer");
        footer.add(
                new Span(getTranslation("login.copyright")),
                new RouterLink(getTranslation("nav.imprint"), ImprintView.class),
                new RouterLink(getTranslation("nav.privacy"), PrivacyView.class));
        return footer;
    }

    private Div createFloaters() {
        Div floaters = new Div();
        floaters.addClassName("floaters");
        floaters.getElement().setAttribute("aria-hidden", "true");
        for (String emoji : FLOATERS) {
            Span floater = new Span(emoji);
            floater.addClassName("floater");
            floaters.add(floater);
        }
        return floaters;
    }

    private String firstName(User user) {
        return user.getDisplayName().split(" ")[0];
    }

    private String roleIcon(Role role) {
        if (role == Role.RESTAURANT) {
            return "🏠";
        }
        if (role == Role.DRIVER) {
            return "🚲";
        }
        return "👤";
    }

    private void navigateByRole(Role role) {
        if (role == Role.CUSTOMER) {
            UI.getCurrent().navigate(CustomerDiscoverView.class);
        } else if (role == Role.RESTAURANT) {
            UI.getCurrent().navigate(RestaurantDashboardView.class);
        } else {
            UI.getCurrent().navigate(DriverDashboardView.class);
        }
    }
}
