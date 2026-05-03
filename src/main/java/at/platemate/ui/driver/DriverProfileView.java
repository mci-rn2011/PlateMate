package at.platemate.ui.driver;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import at.platemate.auth.MockSessionService;
import at.platemate.delivery.Delivery;
import at.platemate.delivery.DeliveryService;
import at.platemate.delivery.DriverProfile;
import at.platemate.ui.layout.MainLayout;
import at.platemate.user.User;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "driver/profile", layout = MainLayout.class)
@PageTitle("Driver Profile | PlateMate")
public class DriverProfileView extends VerticalLayout {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final DeliveryService deliveryService;
    private final MockSessionService sessionService;

    public DriverProfileView(DeliveryService deliveryService, MockSessionService sessionService) {
        this.deliveryService = deliveryService;
        this.sessionService = sessionService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames("pm-driver-page", "pm-driver-profile-page");
        render();
    }

    private void render() {
        sessionService.getCurrentUser().ifPresentOrElse(driver -> {
            add(createHeader(driver), createHistory(driver));
        }, () -> add(emptyState(getTranslation("restaurant.dashboard.session.empty.title"),
                getTranslation("restaurant.dashboard.session.empty.detail"))));
    }

    private Div createHeader(User driver) {
        DriverProfile profile = deliveryService.getDriverProfile(driver);
        DeliveryService.EarningsSummary earnings = deliveryService.getEarningsSummary(driver);

        Div header = new Div();
        header.addClassName("pm-driver-hero");
        Div copy = new Div();
        copy.addClassName("pm-driver-hero-copy");
        Span eyebrow = new Span(getTranslation("driver.profile.eyebrow"));
        eyebrow.addClassName("pm-eyebrow");
        copy.add(eyebrow, new H1(getTranslation("driver.profile.title")),
                new Paragraph(getTranslation("driver.profile.intro")));

        Div summary = new Div();
        summary.addClassName("pm-driver-console");
        summary.add(avatar(profile, driver), new H2(driver.getDisplayName()),
                statusPill(profile), metric(getTranslation("driver.dashboard.metric.today"), money(earnings.todayEarnings())),
                metric(getTranslation("driver.dashboard.metric.total"), money(earnings.totalEarnings())),
                metric(getTranslation("driver.dashboard.metric.completed"), String.valueOf(earnings.completedDeliveries())),
                metric(getTranslation("driver.dashboard.metric.average"), money(earnings.averageDeliveryFee())));
        header.add(copy, summary);
        return header;
    }

    private Div createHistory(User driver) {
        Div history = new Div();
        history.addClassName("pm-driver-history");
        history.add(new H2(getTranslation("driver.profile.history")));
        List<Delivery> deliveries = deliveryService.getEarningsSummary(driver).history();
        if (deliveries.isEmpty()) {
            history.add(emptyState(getTranslation("driver.profile.empty.title"),
                    getTranslation("driver.profile.empty.detail")));
            return history;
        }
        deliveries.forEach(delivery -> history.add(historyCard(delivery)));
        return history;
    }

    private Div historyCard(Delivery delivery) {
        Div card = new Div();
        card.addClassName("pm-driver-card");
        card.add(new H3(getTranslation("driver.dashboard.order", delivery.getOrder().getId())));
        Div grid = new Div();
        grid.addClassName("pm-driver-contact-grid");
        grid.add(info(getTranslation("grid.restaurant"), delivery.getOrder().getRestaurant().getName()));
        grid.add(info(getTranslation("driver.dashboard.dropoff"), delivery.getOrder().getDeliveryAddress()));
        grid.add(info(getTranslation("driver.dashboard.distance"), delivery.getDistanceKm() + " km"));
        grid.add(info(getTranslation("driver.dashboard.metric.average"), money(delivery.getDeliveryFee())));
        grid.add(info(getTranslation("driver.profile.proof"), delivery.getProofType() == null
                ? getTranslation("driver.profile.noProof")
                : getTranslation("proofType." + delivery.getProofType().name())));
        if (delivery.getDeliveredAt() != null) {
            grid.add(info(getTranslation("grid.created"), delivery.getDeliveredAt().format(DATE_TIME)));
        }
        card.add(grid);
        if (delivery.getProofImageUrl() != null) {
            Image image = new Image(delivery.getProofImageUrl(), getTranslation("driver.profile.proof"));
            image.addClassName("pm-proof-preview");
            card.add(image);
        }
        return card;
    }

    private Div metric(String label, String value) {
        Div metric = new Div();
        metric.addClassName("pm-driver-metric");
        Span kicker = new Span(label);
        kicker.addClassName("pm-driver-kicker");
        Span amount = new Span(value);
        amount.addClassName("pm-driver-value");
        metric.add(kicker, amount);
        return metric;
    }

    private Div info(String label, String value) {
        Div info = new Div();
        info.addClassName("pm-driver-metric");
        Span kicker = new Span(label);
        kicker.addClassName("pm-driver-kicker");
        info.add(kicker, new Span(value == null || value.isBlank() ? "-" : value));
        return info;
    }

    private Span statusPill(DriverProfile profile) {
        Span pill = new Span(getTranslation("driverStatus." + profile.getStatus().name()));
        pill.addClassNames("pm-status-pill", switch (profile.getStatus()) {
            case OFFLINE -> "is-closed";
            case BUSY -> "is-busy";
            case AVAILABLE -> "is-open";
        });
        return pill;
    }

    private Span avatar(DriverProfile profile, User driver) {
        Span wrapper = new Span();
        if (profile.getProfileImageUrl() != null) {
            Image image = new Image(profile.getProfileImageUrl(), driver.getDisplayName());
            image.addClassName("pm-driver-avatar");
            wrapper.add(image);
            return wrapper;
        }
        wrapper.setText("🚲");
        wrapper.addClassName("pm-driver-avatar");
        return wrapper;
    }

    private String money(BigDecimal amount) {
        return amount == null ? "0 €" : amount + " €";
    }

    private Div emptyState(String title, String detail) {
        Div empty = new Div();
        empty.addClassName("pm-empty-state");
        empty.add(new H2(title), new Paragraph(detail));
        return empty;
    }
}
