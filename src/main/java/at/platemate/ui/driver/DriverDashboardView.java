package at.platemate.ui.driver;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import at.platemate.auth.MockSessionService;
import at.platemate.delivery.Delivery;
import at.platemate.delivery.DeliveryService;
import at.platemate.delivery.DeliveryStatus;
import at.platemate.delivery.DriverProfile;
import at.platemate.delivery.DriverStatus;
import at.platemate.order.CustomerOrder;
import at.platemate.order.OrderEventBroadcaster;
import at.platemate.ui.layout.MainLayout;
import at.platemate.upload.UploadStorageService;
import at.platemate.user.User;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

@Route(value = "driver/dashboard", layout = MainLayout.class)
@RouteAlias(value = "driver/deliveries", layout = MainLayout.class)
@PageTitle("Driver Dashboard | PlateMate")
public class DriverDashboardView extends VerticalLayout {

    private final DeliveryService deliveryService;
    private final MockSessionService sessionService;
    private final OrderEventBroadcaster orderEventBroadcaster;
    private final UploadStorageService uploadStorageService;
    private final Div board = new Div();
    private final Div side = new Div();
    private OrderEventBroadcaster.Registration registration;

    public DriverDashboardView(
            DeliveryService deliveryService,
            MockSessionService sessionService,
            OrderEventBroadcaster orderEventBroadcaster,
            UploadStorageService uploadStorageService) {
        this.deliveryService = deliveryService;
        this.sessionService = sessionService;
        this.orderEventBroadcaster = orderEventBroadcaster;
        this.uploadStorageService = uploadStorageService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames("pm-driver-page", "pm-driver-dashboard-page");
        render();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        registration = orderEventBroadcaster.subscribeToAll(() -> ui.access(this::render));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    private void render() {
        removeAll();
        sessionService.getCurrentUser().ifPresentOrElse(driver -> {
            add(createHeader(driver));
            Div shell = new Div();
            shell.addClassName("pm-driver-shell");
            board.addClassName("pm-driver-board");
            side.addClassName("pm-driver-side");
            shell.add(board, side);
            add(shell);
            refreshBoard(driver);
        }, () -> add(emptyState(getTranslation("restaurant.dashboard.session.empty.title"),
                getTranslation("restaurant.dashboard.session.empty.detail"))));
    }

    private Div createHeader(User driver) {
        DriverProfile profile = deliveryService.getDriverProfile(driver);
        Div header = new Div();
        header.addClassName("pm-driver-hero");

        Div copy = new Div();
        copy.addClassName("pm-driver-hero-copy");
        Span eyebrow = new Span(getTranslation("driver.dashboard.eyebrow"));
        eyebrow.addClassName("pm-eyebrow");
        H1 title = new H1(getTranslation("driver.dashboard.title"));
        Paragraph intro = new Paragraph(getTranslation("driver.dashboard.intro"));
        Div identity = new Div();
        identity.addClassName("pm-driver-stat-row");
        identity.add(avatar(profile, driver), new Span(driver.getDisplayName()));
        copy.add(eyebrow, title, intro, identity);

        Div console = new Div();
        console.addClassName("pm-driver-console");
        console.add(new H2(getTranslation("driver.dashboard.status.title")),
                new Paragraph(getTranslation("driver.dashboard.status.description")),
                statusPill(profile.getStatus()),
                new Span(getTranslation("driver.dashboard.status.activeLimit",
                        deliveryService.activeDeliveryCount(driver), profile.getActiveDeliveryLimit())));
        Div switcher = new Div();
        switcher.addClassName("pm-driver-status-switcher");
        switcher.add(statusButton(driver, profile, DriverStatus.AVAILABLE));
        switcher.add(statusButton(driver, profile, DriverStatus.OFFLINE));
        console.add(switcher);

        header.add(copy, console);
        return header;
    }

    private Button statusButton(User driver, DriverProfile profile, DriverStatus status) {
        Button button = new Button(getTranslation("driverStatus." + status.name()), event -> runAndRender(() ->
                deliveryService.updateDriverAvailability(driver, status)));
        button.addClassName("pm-driver-status-action");
        if (profile.getStatus() == status) {
            button.addClassName("is-active");
            button.setEnabled(false);
        }
        return button;
    }

    private void refreshBoard(User driver) {
        board.removeAll();
        side.removeAll();

        List<Delivery> deliveries = deliveryService.findDeliveries(driver).stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.ASSIGNED
                        || delivery.getStatus() == DeliveryStatus.ACCEPTED_BY_DRIVER
                        || delivery.getStatus() == DeliveryStatus.PICKED_UP
                        || delivery.getStatus() == DeliveryStatus.ON_THE_WAY)
                .toList();
        List<Delivery> incoming = deliveries.stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.ASSIGNED)
                .toList();
        List<Delivery> active = deliveries.stream()
                .filter(delivery -> delivery.getStatus() != DeliveryStatus.ASSIGNED)
                .toList();

        board.add(new H2(getTranslation("driver.dashboard.section.incoming")));
        if (incoming.isEmpty()) {
            board.add(emptyState(getTranslation("driver.dashboard.section.empty.title"),
                    getTranslation("driver.dashboard.section.empty.detail")));
        } else {
            incoming.forEach(delivery -> board.add(createDeliveryCard(delivery, true)));
        }

        board.add(new H2(getTranslation("driver.dashboard.section.active")));
        active.forEach(delivery -> board.add(createDeliveryCard(delivery, false)));

        DeliveryService.EarningsSummary earnings = deliveryService.getEarningsSummary(driver);
        side.add(metric(getTranslation("driver.dashboard.metric.today"), money(earnings.todayEarnings())));
        side.add(metric(getTranslation("driver.dashboard.metric.total"), money(earnings.totalEarnings())));
        side.add(metric(getTranslation("driver.dashboard.metric.completed"), String.valueOf(earnings.completedDeliveries())));
        side.add(metric(getTranslation("driver.dashboard.metric.average"), money(earnings.averageDeliveryFee())));
        side.add(recentDropoffs(earnings.history()));
    }

    private Div recentDropoffs(List<Delivery> history) {
        Div recent = new Div();
        recent.addClassName("pm-driver-card");
        recent.add(new H3(getTranslation("driver.dashboard.recentDropoffs")));
        List<Delivery> topThree = history.stream().limit(3).toList();
        if (topThree.isEmpty()) {
            recent.add(new Paragraph(getTranslation("driver.profile.empty.detail")));
            return recent;
        }
        topThree.forEach(delivery -> recent.add(createDropoffRow(delivery)));
        return recent;
    }

    private Div createDropoffRow(Delivery delivery) {
        Div row = new Div();
        row.addClassName("pm-dropoff-row");
        Div copy = new Div();
        copy.addClassName("pm-dropoff-copy");
        copy.add(new Span(getTranslation("driver.dashboard.order", delivery.getOrder().getId())),
                new Span(delivery.getOrder().getRestaurant().getName() + " · " + money(delivery.getDeliveryFee())),
                new Span(getTranslation("driver.dashboard.dropoffCompleted",
                        delivery.getDriver().getDisplayName())));
        Button inspect = new Button(getTranslation("driver.dashboard.inspectDropoff"),
                event -> openDropoffDialog(delivery));
        inspect.addClassName("pm-soft-action");
        row.add(copy, inspect);
        return row;
    }

    private void openDropoffDialog(Delivery delivery) {
        CustomerOrder order = delivery.getOrder();
        Dialog dialog = new Dialog();
        dialog.addClassName("pm-editor-dialog");
        dialog.setWidth("min(860px, calc(100vw - 2rem))");
        dialog.setHeaderTitle(getTranslation("driver.dashboard.order", order.getId()));

        Div content = new Div();
        content.addClassName("pm-dropoff-detail");
        content.add(info(getTranslation("driver.dashboard.pickup"), order.getRestaurant().getName() + " · " + order.getRestaurant().getAddress()));
        content.add(info(getTranslation("driver.dashboard.dropoff"), nullToDash(order.getDeliveryAddress())));
        content.add(info(getTranslation("driver.dashboard.customer"), customerName(order)));
        content.add(info(getTranslation("driver.dashboard.contact"), contact(order)));
        content.add(info(getTranslation("driver.dashboard.fee"), money(delivery.getDeliveryFee())));
        if (delivery.getProofType() != null) {
            content.add(info(getTranslation("driver.profile.proof"),
                    getTranslation("proofType." + delivery.getProofType().name())));
        }
        if (delivery.getProofImageUrl() != null) {
            Image image = new Image(delivery.getProofImageUrl(), getTranslation("driver.dashboard.proof.photoReady"));
            image.addClassName("pm-proof-preview");
            content.add(image);
        }
        dialog.add(content);
        dialog.getFooter().add(new Button(getTranslation("action.close"), event -> dialog.close()));
        dialog.open();
    }

    private Div createDeliveryCard(Delivery delivery, boolean incoming) {
        CustomerOrder order = delivery.getOrder();
        Div card = new Div();
        card.addClassNames("pm-driver-card", incoming ? "is-incoming" : "is-active-delivery");

        Div top = new Div();
        top.addClassName("pm-driver-card-top");
        top.add(new H3(getTranslation("driver.dashboard.order", order.getId())),
                statusPill(delivery.getStatus()));

        Div details = new Div();
        details.addClassName("pm-driver-contact-grid");
        details.add(info(getTranslation("driver.dashboard.pickup"), order.getRestaurant().getName() + " · " + order.getRestaurant().getAddress()));
        details.add(info(getTranslation("driver.dashboard.dropoff"), nullToDash(order.getDeliveryAddress())));
        details.add(info(getTranslation("driver.dashboard.customer"), customerName(order)));
        details.add(info(getTranslation("driver.dashboard.contact"), contact(order)));
        if (order.getDeliveryInstructions() != null) {
            details.add(info(getTranslation("driver.dashboard.note"), order.getDeliveryInstructions()));
        }

        card.add(top, routeCard(delivery), details, actions(delivery));
        if (!incoming && delivery.getStatus() == DeliveryStatus.ON_THE_WAY) {
            card.add(proofPanel(delivery));
        }
        return card;
    }

    private Div routeCard(Delivery delivery) {
        Div route = new Div();
        route.addClassName("pm-route-card");
        route.add(new Span(getTranslation("driver.dashboard.route")));
        if (delivery.getRoutePreviewUrl() != null && !delivery.getRoutePreviewUrl().isBlank()) {
            Image map = new Image(delivery.getRoutePreviewUrl(), getTranslation("customer.profile.routeAlt"));
            map.addClassName("pm-route-preview-image");
            route.add(map);
        } else {
            Div line = new Div();
            line.addClassName("pm-route-line");
            line.add(new Span("🏪"), new Span(), new Span("📍"));
            line.getComponentAt(1).getElement().getClassList().add("pm-route-track");
            route.add(line);
        }
        Div stats = new Div();
        stats.addClassName("pm-route-stats");
        stats.add(new Span(getTranslation("driver.dashboard.distance", delivery.getDistanceKm())),
                new Span(getTranslation("driver.dashboard.duration", delivery.getEstimatedMinutes())),
                new Span(getTranslation("driver.dashboard.fee", money(delivery.getDeliveryFee()))));
        route.add(stats);
        return route;
    }

    private Div actions(Delivery delivery) {
        Div actions = new Div();
        actions.addClassName("pm-proof-actions");
        if (delivery.getStatus() == DeliveryStatus.ASSIGNED) {
            Button accept = new Button(getTranslation("action.accept"),
                    event -> runAndRender(() -> deliveryService.acceptDelivery(delivery.getId())));
            accept.addClassName("pm-primary-action");
            Button reject = new Button(getTranslation("action.reject"),
                    event -> runAndRender(() -> deliveryService.rejectDelivery(delivery.getId())));
            reject.addClassName("pm-danger-action");
            actions.add(accept, reject);
        }
        if (delivery.getStatus() == DeliveryStatus.ACCEPTED_BY_DRIVER) {
            Button pickedUp = new Button(getTranslation("driver.dashboard.action.startDelivery"),
                    event -> runAndRender(() -> deliveryService.markPickedUp(delivery.getId())));
            pickedUp.addClassName("pm-primary-action");
            actions.add(pickedUp);
        }
        if (delivery.getStatus() == DeliveryStatus.PICKED_UP) {
            Button onWay = new Button(getTranslation("driver.dashboard.action.startDelivery"),
                    event -> runAndRender(() -> deliveryService.markOnTheWay(delivery.getId())));
            onWay.addClassName("pm-primary-action");
            actions.add(onWay);
        }
        if (delivery.getStatus() == DeliveryStatus.ON_THE_WAY) {
            Button complete = new Button(getTranslation("driver.dashboard.action.completeDropoff"),
                    event -> runAndRender(() -> deliveryService.markDelivered(delivery.getId())));
            complete.addClassName("pm-primary-action");
            actions.add(complete);
        }
        return actions;
    }

    private Div proofPanel(Delivery delivery) {
        Div panel = new Div();
        panel.addClassName("pm-proof-panel");
        panel.add(new H3(getTranslation("driver.dashboard.proof.title")),
                new Paragraph(getTranslation("driver.dashboard.proof.detail")));

        TextField code = new TextField(getTranslation("driver.dashboard.proof.code"));
        Button verify = new Button(getTranslation("driver.dashboard.proof.verify"), event -> runAndRender(() ->
                deliveryService.completeDropoffWithQr(delivery.getId(), code.getValue())));
        verify.addClassName("pm-soft-action");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/webp");
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span(getTranslation("driver.dashboard.proof.photo")));
        upload.addSucceededListener(event -> runAndRender(() -> {
            String path = uploadStorageService.saveDeliveryProof(
                    delivery.getId(),
                    event.getFileName(),
                    event.getMIMEType(),
                    event.getContentLength(),
                    buffer.getInputStream(),
                    delivery.getProofImageUrl());
            deliveryService.completeDropoffWithPhoto(delivery.getId(), path);
        }));

        if (delivery.getProofImageUrl() != null) {
            Image image = new Image(delivery.getProofImageUrl(), getTranslation("driver.dashboard.proof.photoReady"));
            image.addClassName("pm-proof-preview");
            panel.add(image);
        }
        panel.add(code, verify, upload);
        return panel;
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
        info.add(kicker, new Span(nullToDash(value)));
        return info;
    }

    private Span statusPill(DriverStatus status) {
        Span pill = new Span(getTranslation("driverStatus." + status.name()));
        pill.addClassNames("pm-status-pill", status == DriverStatus.OFFLINE ? "is-closed" : status == DriverStatus.BUSY ? "is-busy" : "is-open");
        return pill;
    }

    private Span statusPill(DeliveryStatus status) {
        Span pill = new Span(getTranslation("deliveryStatus." + status.name()));
        pill.addClassNames("pm-status-pill", status == DeliveryStatus.REJECTED_BY_DRIVER ? "is-closed" : "is-open");
        return pill;
    }

    private Span avatar(DriverProfile profile, User driver) {
        if (profile.getProfileImageUrl() != null) {
            Image image = new Image(profile.getProfileImageUrl(), driver.getDisplayName());
            image.addClassName("pm-driver-avatar");
            return new Span(image);
        }
        return new Span("🚲");
    }

    private String customerName(CustomerOrder order) {
        if (order.getContactName() != null) {
            return order.getContactName();
        }
        return order.getCustomer() == null ? "-" : order.getCustomer().getDisplayName();
    }

    private String contact(CustomerOrder order) {
        if (order.getContactPhone() != null && order.getContactEmail() != null) {
            return order.getContactPhone() + " · " + order.getContactEmail();
        }
        if (order.getContactPhone() != null) {
            return order.getContactPhone();
        }
        return nullToDash(order.getContactEmail());
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
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

    private void runAndRender(Runnable action) {
        try {
            action.run();
            render();
        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage());
        }
    }
}
