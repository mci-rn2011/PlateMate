package at.platemate.ui.customer;

import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import at.platemate.auth.MockSessionService;
import at.platemate.delivery.Delivery;
import at.platemate.delivery.DeliveryRepository;
import at.platemate.delivery.QrCodeService;
import at.platemate.order.CustomerOrder;
import at.platemate.order.OrderEventBroadcaster;
import at.platemate.order.OrderItem;
import at.platemate.order.OrderService;
import at.platemate.order.OrderStatus;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

@Route(value = "customer/profile", layout = MainLayout.class)
@PageTitle("Profile | PlateMate")
public class CustomerProfileView extends VerticalLayout {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final EnumSet<OrderStatus> QR_ELIGIBLE_STATUSES = EnumSet.of(
            OrderStatus.ACCEPTED,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.OUT_FOR_DELIVERY);

    private final OrderService orderService;
    private final MockSessionService sessionService;
    private final OrderEventBroadcaster orderEventBroadcaster;
    private final DeliveryRepository deliveryRepository;
    private final QrCodeService qrCodeService;
    private final Div ordersSection = new Div();
    private Dialog activeOrderDialog;
    private Long activeDialogOrderId;
    private OrderEventBroadcaster.Registration registration;

    public CustomerProfileView(
            OrderService orderService,
            MockSessionService sessionService,
            OrderEventBroadcaster orderEventBroadcaster,
            DeliveryRepository deliveryRepository,
            QrCodeService qrCodeService) {
        this.orderService = orderService;
        this.sessionService = sessionService;
        this.orderEventBroadcaster = orderEventBroadcaster;
        this.deliveryRepository = deliveryRepository;
        this.qrCodeService = qrCodeService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames("pm-customer-page", "pm-profile-page");

        ordersSection.addClassName("pm-order-history");
        add(createHeader(), ordersSection);
        refreshOrders();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        registration = orderEventBroadcaster.subscribeToAll(() -> ui.access(this::refreshOrders));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassNames("pm-customer-hero", "pm-profile-hero");
        Span eyebrow = new Span(getTranslation("customer.profile.eyebrow"));
        eyebrow.addClassName("pm-eyebrow");
        H1 title = new H1(sessionService.getCurrentUser()
                .map(user -> user.getDisplayName())
                .orElse(getTranslation("customer.profile.guest")));
        Paragraph intro = new Paragraph(sessionService.isGuest()
                ? getTranslation("customer.profile.guestIntro")
                : getTranslation("customer.profile.intro"));
        header.add(eyebrow, title, intro);
        return header;
    }

    private void refreshOrders() {
        ordersSection.removeAll();
        List<CustomerOrder> orders = sessionService.isGuest()
                ? orderService.findGuestSessionOrders(sessionService.getGuestSessionId())
                : sessionService.getCurrentUser()
                .map(orderService::findCustomerOrders)
                .orElse(List.of());

        if (orders.isEmpty()) {
            Div empty = new Div();
            empty.addClassName("pm-empty-state");
            empty.add(new H2(getTranslation("customer.profile.empty.title")),
                    new Paragraph(getTranslation("customer.profile.empty.detail")));
            ordersSection.add(empty);
            return;
        }

        List<CustomerOrder> active = orders.stream().filter(this::isActiveOrder).toList();
        List<CustomerOrder> history = orders.stream().filter(order -> !isActiveOrder(order)).toList();
        if (!active.isEmpty()) {
            ordersSection.add(new H2(getTranslation("customer.profile.activeOrders")));
            active.forEach(order -> ordersSection.add(createOrderCard(order, true)));
        }
        if (!history.isEmpty()) {
            ordersSection.add(new H2(getTranslation("customer.profile.orders")));
            history.forEach(order -> ordersSection.add(createOrderCard(order, false)));
        }
        refreshOpenDialog();
    }

    private Div createOrderCard(CustomerOrder order, boolean active) {
        Div card = new Div();
        card.addClassName("pm-order-preview-card");
        if (active) {
            card.addClassName("is-active-order");
        }

        Div top = new Div();
        top.addClassName("pm-card-title-row");
        top.add(new H3(getTranslation("customer.profile.orderNumber", order.getId())), statusPill(order.getStatus()));

        int totalQuantity = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        Button details = new Button(getTranslation("customer.profile.viewOrder"), event -> openOrderDialog(order));
        details.addClassName("pm-primary-action");
        Button reorder = new Button(getTranslation("customer.profile.orderAgain", order.getRestaurant().getName()),
                event -> getUI().ifPresent(ui -> ui.navigate(CustomerMenuView.class,
                        new RouteParameters("restaurantId", order.getRestaurant().getId().toString()))));
        reorder.addClassName("pm-soft-action");

        Div actions = new Div(reorder, details);
        actions.addClassNames("pm-proof-actions", "pm-order-card-actions");
        card.add(top,
                compactLine(order.getRestaurant().getName() + " · " + order.getCreatedAt().format(DATE_TIME)),
                compactLine(getTranslation("customer.profile.orderItems", totalQuantity, money(order.getTotalPrice()))),
                actions);
        return card;
    }

    private void openOrderDialog(CustomerOrder order) {
        try {
            CustomerOrder freshOrder = orderService.getOrder(order.getId());
            Dialog dialog = new Dialog();
            activeOrderDialog = dialog;
            activeDialogOrderId = freshOrder.getId();
            dialog.addClassName("pm-editor-dialog");
            dialog.setWidth("min(980px, calc(100vw - 2rem))");
            dialog.setHeaderTitle(getTranslation("customer.profile.orderNumber", freshOrder.getId()));
            dialog.addDetachListener(event -> clearActiveDialog(dialog));
            dialog.addOpenedChangeListener(event -> {
                if (!event.isOpened()) {
                    clearActiveDialog(dialog);
                }
            });

            dialog.add(createOrderDialogContent(freshOrder));
            Button close = new Button(getTranslation("action.close"), event -> dialog.close());
            dialog.getFooter().add(close);
            dialog.open();
        } catch (RuntimeException ex) {
            Notification.show(getTranslation("customer.profile.openFailed"));
        }
    }

    private Div createOrderDialogContent(CustomerOrder order) {
        Div content = new Div();
        content.addClassName("pm-order-detail");
        content.add(new H2(order.getRestaurant().getName()), statusPill(order.getStatus()));
        content.add(compactLine(order.getCreatedAt().format(DATE_TIME)));
        if (!order.getFullDeliveryAddress().isBlank()) {
            content.add(compactLine(getTranslation("customer.checkout.address") + ": " + order.getFullDeliveryAddress()));
        }
        if (order.getDeliveryAddressNormalized() != null && !order.getDeliveryAddressNormalized().isBlank()) {
            content.add(compactLine(getTranslation("customer.profile.normalizedAddress") + ": "
                    + order.getDeliveryAddressNormalized()));
        }
        if (order.getDeliveryInstructions() != null && !order.getDeliveryInstructions().isBlank()) {
            content.add(compactLine(getTranslation("customer.checkout.note") + ": " + order.getDeliveryInstructions()));
        }

        content.add(createDriverPanel(order), createRoutePreview(order), createProofPanel(order));

        for (OrderItem item : order.getItems()) {
            Div line = new Div();
            line.addClassName("pm-order-detail-line");
            line.add(new Span(item.getQuantity() + " × " + item.getItemName()), new Span(money(item.getLineTotal())));
            content.add(line);
        }
        Div total = new Div(new Span(getTranslation("customer.checkout.total")), new Span(money(order.getTotalPrice())));
        total.addClassName("pm-total-row");
        content.add(total);
        return content;
    }

    private Div createRoutePreview(CustomerOrder order) {
        Div route = new Div();
        route.addClassName("pm-route-placeholder");
        Optional<String> previewUrl = Optional.ofNullable(order.getRoutePreviewUrl())
                .filter(url -> !url.isBlank())
                .or(() -> deliveryFor(order).map(Delivery::getRoutePreviewUrl).filter(url -> !url.isBlank()));
        if (previewUrl.isPresent()) {
            Image map = new Image(previewUrl.get(), getTranslation("customer.profile.routeAlt"));
            map.addClassName("pm-route-preview-image");
            route.add(map);
        } else {
            route.add(new Span("🛵"), new Paragraph(getTranslation("customer.profile.routePlaceholder")));
        }
        return route;
    }

    private void refreshOpenDialog() {
        if (activeOrderDialog == null || activeDialogOrderId == null || !activeOrderDialog.isOpened()) {
            return;
        }
        try {
            CustomerOrder refreshed = orderService.getOrder(activeDialogOrderId);
            activeOrderDialog.removeAll();
            activeOrderDialog.add(createOrderDialogContent(refreshed));
        } catch (RuntimeException ex) {
            Notification.show(getTranslation("customer.profile.refreshFailed"));
        }
    }

    private void clearActiveDialog(Dialog dialog) {
        if (activeOrderDialog == dialog) {
            activeOrderDialog = null;
            activeDialogOrderId = null;
        }
    }

    private Div createDriverPanel(CustomerOrder order) {
        Div panel = new Div();
        panel.addClassName("pm-delivery-integration-panel");

        Div driver = new Div();
        driver.addClassName("pm-delivery-info-card");
        driver.add(new H3(getTranslation("customer.profile.driver.title")));
        if (order.getAssignedDriver() != null) {
            driver.add(new Paragraph(getTranslation("customer.profile.driver.assigned",
                    order.getAssignedDriver().getDisplayName())));
        } else {
            driver.add(new Paragraph(getTranslation("customer.profile.driver.pending")));
        }

        panel.add(driver, createQrCard(order));
        return panel;
    }

    private Div createQrCard(CustomerOrder order) {
        Div qrCard = new Div();
        qrCard.addClassName("pm-delivery-info-card");
        qrCard.add(new H3(getTranslation("customer.profile.qr.title")));

        if (!QR_ELIGIBLE_STATUSES.contains(order.getStatus())) {
            qrCard.add(new Paragraph(getTranslation("customer.profile.qr.hidden")));
            return qrCard;
        }

        Optional<String> token = deliveryFor(order).map(Delivery::getConfirmationCode);
        if (token.isPresent()) {
            Image qr = new Image(qrCodeService.createPngDataUrl(token.get()), getTranslation("customer.profile.qr.title"));
            qr.addClassName("pm-qr-image");
            qrCard.add(qr);
            qrCard.add(new Paragraph(getTranslation("customer.profile.qr.ready")));
            qrCard.add(compactLine(token.get()));
        } else {
            Div qr = new Div();
            qr.addClassName("pm-qr-code is-pending");
            qr.add(new Span("QR"));
            qrCard.add(qr, new Paragraph(getTranslation("customer.profile.qr.pending")));
        }
        return qrCard;
    }

    private Div createProofPanel(CustomerOrder order) {
        Div panel = new Div();
        panel.addClassName("pm-proof-panel");
        panel.add(new H3(getTranslation("customer.profile.proof.title")));

        Optional<Delivery> delivery = deliveryFor(order);
        Optional<String> proofType = delivery.map(Delivery::getProofType).map(Enum::name);
        Optional<String> proofImageUrl = delivery.map(Delivery::getProofImageUrl);

        if (proofImageUrl.isPresent()) {
            Image proof = new Image(proofImageUrl.get(), getTranslation("customer.profile.proof.imageAlt"));
            proof.addClassName("pm-proof-image");
            panel.add(proof);
        }
        panel.add(new Paragraph(proofType
                .map(type -> getTranslation("customer.profile.proof.available", proofLabel(type)))
                .orElseGet(() -> getTranslation("customer.profile.proof.pending"))));
        return panel;
    }

    private Optional<Delivery> deliveryFor(CustomerOrder order) {
        return deliveryRepository.findByOrder(order);
    }

    private Span statusPill(OrderStatus status) {
        Span pill = new Span(getTranslation("orderStatus." + status.name()));
        pill.addClassNames("pm-status-pill", switch (status) {
            case CANCELLED, REJECTED -> "is-closed";
            case PAYMENT_PENDING -> "is-busy";
            default -> "is-open";
        });
        return pill;
    }

    private boolean isActiveOrder(CustomerOrder order) {
        return order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.REJECTED;
    }

    private Paragraph compactLine(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.addClassName("pm-compact-line");
        return paragraph;
    }

    private String proofLabel(String proofType) {
        String normalized = proofType.toUpperCase();
        if (normalized.contains("QR")) {
            return getTranslation("customer.profile.proof.qr");
        }
        if (normalized.contains("PHOTO")) {
            return getTranslation("customer.profile.proof.photo");
        }
        return proofType;
    }

    private String money(Object amount) {
        return amount + " €";
    }
}
