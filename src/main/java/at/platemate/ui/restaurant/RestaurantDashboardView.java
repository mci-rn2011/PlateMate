package at.platemate.ui.restaurant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import at.platemate.auth.MockSessionService;
import at.platemate.delivery.Delivery;
import at.platemate.delivery.DeliveryService;
import at.platemate.delivery.DeliveryStatus;
import at.platemate.order.CustomerOrder;
import at.platemate.order.OrderEventBroadcaster;
import at.platemate.order.OrderItem;
import at.platemate.order.OrderService;
import at.platemate.order.OrderStatus;
import at.platemate.restaurant.Restaurant;
import at.platemate.restaurant.RestaurantService;
import at.platemate.restaurant.RestaurantStatus;
import at.platemate.ui.layout.MainLayout;
import at.platemate.user.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "restaurant/dashboard", layout = MainLayout.class)
@PageTitle("Dashboard | PlateMate")
public class RestaurantDashboardView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final EnumSet<OrderStatus> ACTIVE_STATUSES = EnumSet.of(
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.PLACED,
            OrderStatus.ACCEPTED,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.OUT_FOR_DELIVERY);
    private final OrderService orderService;
    private final RestaurantService restaurantService;
    private final DeliveryService deliveryService;
    private final MockSessionService sessionService;
    private final OrderEventBroadcaster orderEventBroadcaster;
    private final VerticalLayout metrics = new VerticalLayout();
    private final Div board = new Div();
    private OrderEventBroadcaster.Registration registration;
    private OrderEventBroadcaster.Registration globalRegistration;

    private boolean showingPastOrders;

    public RestaurantDashboardView(
            OrderService orderService,
            RestaurantService restaurantService,
            DeliveryService deliveryService,
            MockSessionService sessionService,
            OrderEventBroadcaster orderEventBroadcaster) {
        this.orderService = orderService;
        this.restaurantService = restaurantService;
        this.deliveryService = deliveryService;
        this.sessionService = sessionService;
        this.orderEventBroadcaster = orderEventBroadcaster;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("pm-restaurant-page");

        add(createHeader());
        add(createDashboardBody());
        refresh();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        scaffoldPushSubscription();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
        if (globalRegistration != null) {
            globalRegistration.unregister();
            globalRegistration = null;
        }
    }

    private Div createHeader() {
        Div header = new Div();
        header.addClassNames("pm-restaurant-hero", "pm-dashboard-hero");

        Div copy = new Div();
        copy.addClassName("pm-restaurant-hero-copy");
        Span eyebrow = new Span(getTranslation("restaurant.dashboard.eyebrow"));
        eyebrow.addClassName("pm-eyebrow");
        H1 title = new H1(getTranslation("restaurant.dashboard.title"));
        Paragraph intro = new Paragraph(getTranslation("restaurant.dashboard.intro"));
        copy.add(eyebrow, title, intro);

        Div controls = new Div();
        controls.addClassName("pm-status-console");
        Restaurant currentRestaurant = sessionService.getCurrentUser()
                .flatMap(user -> restaurantService.findForOwner(user).stream().findFirst())
                .orElse(null);
        RestaurantStatus currentStatus = currentRestaurant == null ? RestaurantStatus.CLOSED : currentRestaurant.getStatus();
        controls.add(new H2(getTranslation("restaurant.dashboard.status.title")));
        controls.add(new Paragraph(getTranslation("restaurant.dashboard.status.description")));
        controls.add(createCurrentStatusPanel(currentRestaurant, currentStatus));
        Div switcher = new Div();
        switcher.addClassName("pm-status-switcher");
        switcher.add(createStatusButton(currentRestaurant, currentStatus, RestaurantStatus.OPEN));
        switcher.add(createStatusButton(currentRestaurant, currentStatus, RestaurantStatus.BUSY));
        switcher.add(createStatusButton(currentRestaurant, currentStatus, RestaurantStatus.CLOSED));
        controls.add(switcher);

        header.add(copy, controls);
        return header;
    }

    private Div createDashboardBody() {
        Div body = new Div();
        body.addClassName("pm-dashboard-body");

        metrics.setPadding(false);
        metrics.setSpacing(false);
        metrics.addClassName("pm-metric-grid");

        Tab active = new Tab(getTranslation("restaurant.dashboard.tabs.active"));
        Tab past = new Tab(getTranslation("restaurant.dashboard.tabs.past"));
        Tabs tabs = new Tabs(active, past);
        tabs.addClassName("pm-order-tabs");
        tabs.addSelectedChangeListener(event -> {
            showingPastOrders = event.getSelectedTab() == past;
            refresh();
        });

        board.addClassName("pm-order-board");
        body.add(metrics, tabs, board);
        return body;
    }

    private Div createCurrentStatusPanel(Restaurant restaurant, RestaurantStatus currentStatus) {
        Div panel = new Div();
        panel.addClassNames("pm-current-status-panel", statusToneClass(currentStatus));
        Span label = new Span(getTranslation("restaurant.dashboard.status.current"));
        label.addClassName("pm-current-status-label");
        Span value = new Span(statusLabel(currentStatus));
        value.addClassName("pm-current-status-value");
        Paragraph hint = new Paragraph(restaurant == null
                ? getTranslation("restaurant.dashboard.live.noRestaurant")
                : getTranslation("restaurant.dashboard.status.hint." + currentStatus.name()));
        panel.add(label, value, hint);
        return panel;
    }

    private Button createStatusButton(Restaurant restaurant, RestaurantStatus currentStatus, RestaurantStatus status) {
        Button button = new Button(statusLabel(status), event -> {
            if (restaurant != null) {
                try {
                    restaurantService.updateStatus(restaurant.getId(), status);
                    replace(getComponentAt(0), createHeader());
                    refresh();
                } catch (RuntimeException ex) {
                    Notification.show(ex.getMessage());
                }
            }
        });
        button.addClassNames("pm-status-action", statusToneClass(status));
        if (currentStatus == status) {
            button.addClassName("is-active");
            button.setEnabled(false);
        }
        return button;
    }

    private void refresh() {
        sessionService.getCurrentUser().ifPresentOrElse(user -> {
            List<Restaurant> restaurants = restaurantService.findForOwner(user);
            List<CustomerOrder> orders = orderService.findRestaurantOrders(restaurants);
            renderMetrics(restaurants, orders);
            renderBoard(orders);
        }, () -> {
            metrics.removeAll();
            board.removeAll();
            board.add(createEmptyState(getTranslation("restaurant.dashboard.session.empty.title"),
                    getTranslation("restaurant.dashboard.session.empty.detail")));
        });
    }

    private void renderMetrics(List<Restaurant> restaurants, List<CustomerOrder> orders) {
        metrics.removeAll();
        List<CustomerOrder> todaysOrders = orders.stream()
                .filter(order -> order.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .toList();
        long placed = count(todaysOrders, OrderStatus.PLACED);
        long preparing = todaysOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.ACCEPTED || order.getStatus() == OrderStatus.PREPARING)
                .count();
        long ready = count(todaysOrders, OrderStatus.READY_FOR_PICKUP);
        BigDecimal todayRevenue = todaysOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(CustomerOrder::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(CustomerOrder::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalDelivered = count(orders, OrderStatus.DELIVERED);
        long totalCancelled = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED)
                .count();

        metrics.add(createMetric(getTranslation("restaurant.dashboard.metric.new"), String.valueOf(placed),
                getTranslation("restaurant.dashboard.metric.newHint")));
        metrics.add(createMetric(getTranslation("restaurant.dashboard.metric.preparing"), String.valueOf(preparing),
                getTranslation("restaurant.dashboard.metric.preparingHint")));
        metrics.add(createMetric(getTranslation("restaurant.dashboard.metric.ready"), String.valueOf(ready),
                getTranslation("restaurant.dashboard.metric.readyHint")));
        metrics.add(createMetric(getTranslation("restaurant.dashboard.metric.todayRevenue"), money(todayRevenue),
                getTranslation("restaurant.dashboard.metric.todayRevenueHint")));
        Div overall = createMetric(getTranslation("restaurant.dashboard.metric.overall"), money(totalRevenue),
                getTranslation("restaurant.dashboard.metric.revenueHint"));
        overall.addClassName("is-secondary");
        metrics.add(overall);
        Div delivered = createMetric(getTranslation("restaurant.dashboard.metric.delivered"), String.valueOf(totalDelivered),
                getTranslation("restaurant.dashboard.metric.deliveredHint"));
        delivered.addClassName("is-secondary");
        metrics.add(delivered);
        Div cancelled = createMetric(getTranslation("restaurant.dashboard.metric.cancelled"), String.valueOf(totalCancelled),
                getTranslation("restaurant.dashboard.metric.cancelledHint"));
        cancelled.addClassName("is-secondary");
        metrics.add(cancelled);
    }

    private void renderBoard(List<CustomerOrder> orders) {
        board.removeAll();
        List<CustomerOrder> visible = orders.stream()
                .filter(order -> showingPastOrders != ACTIVE_STATUSES.contains(order.getStatus()))
                .toList();

        if (visible.isEmpty()) {
            board.add(createEmptyState(showingPastOrders
                    ? getTranslation("restaurant.dashboard.empty.past.title")
                    : getTranslation("restaurant.dashboard.empty.active.title"),
                    showingPastOrders
                            ? getTranslation("restaurant.dashboard.empty.past.detail")
                            : getTranslation("restaurant.dashboard.empty.active.detail")));
            return;
        }

        Map<OrderStatus, List<CustomerOrder>> grouped = visible.stream()
                .collect(Collectors.groupingBy(CustomerOrder::getStatus));

        if (showingPastOrders) {
            addLane(grouped, getTranslation("restaurant.dashboard.lane.past"), OrderStatus.DELIVERED,
                    OrderStatus.REJECTED, OrderStatus.CANCELLED);
            return;
        }

        addLane(grouped, getTranslation("restaurant.dashboard.lane.new"), OrderStatus.PLACED, OrderStatus.PAYMENT_PENDING);
        addLane(grouped, getTranslation("restaurant.dashboard.lane.preparing"), OrderStatus.ACCEPTED, OrderStatus.PREPARING);
        addLane(grouped, getTranslation("restaurant.dashboard.lane.readyForPickup"), OrderStatus.READY_FOR_PICKUP);
        addLane(grouped, getTranslation("restaurant.dashboard.lane.outForDelivery"), OrderStatus.OUT_FOR_DELIVERY);
    }

    private void addLane(Map<OrderStatus, List<CustomerOrder>> grouped, String title, OrderStatus... statuses) {
        Div lane = new Div();
        lane.addClassName("pm-order-lane");
        H2 heading = new H2(title);
        lane.add(heading);
        for (OrderStatus status : statuses) {
            grouped.getOrDefault(status, List.of()).forEach(order -> lane.add(createOrderCard(order)));
        }
        if (lane.getComponentCount() > 1) {
            board.add(lane);
        }
    }

    private Div createOrderCard(CustomerOrder order) {
        Div card = new Div();
        card.addClassName("pm-order-card");

        Div top = new Div();
        top.addClassName("pm-order-card-top");
        H3 id = new H3(getTranslation("restaurant.dashboard.order.number", order.getId()));
        Span status = new Span(getTranslation("orderStatus." + order.getStatus().name()));
        status.addClassNames("pm-status-pill", statusClass(order.getStatus()));
        top.add(id, status);

        Paragraph customer = new Paragraph(order.getCustomer().getDisplayName() + " - "
                + order.getCreatedAt().format(TIME_FORMAT) + " - " + money(order.getTotalPrice()));
        customer.addClassName("pm-order-meta");

        Div items = new Div();
        items.addClassName("pm-order-items");
        for (OrderItem item : order.getItems()) {
            items.add(new Span(item.getQuantity() + "x " + item.getItemName()));
        }

        Div actions = createActions(order);
        card.add(top, customer);
        if (order.getDeliveryInstructions() != null && !order.getDeliveryInstructions().isBlank()) {
            Paragraph note = new Paragraph(getTranslation("restaurant.dashboard.note", order.getDeliveryInstructions()));
            note.addClassName("pm-order-note");
            card.add(note);
        }
        card.add(items, actions);
        return card;
    }

    private Div createActions(CustomerOrder order) {
        Div actions = new Div();
        actions.addClassName("pm-card-actions");

        if (order.getStatus() == OrderStatus.PLACED) {
            actions.add(primaryAction(getTranslation("action.accept"), () -> orderService.acceptOrder(order.getId())));
            actions.add(dangerAction(getTranslation("action.reject"), () -> orderService.rejectOrder(order.getId())));
        }

        if (order.getStatus() == OrderStatus.ACCEPTED || order.getStatus() == OrderStatus.PREPARING) {
            actions.add(primaryAction(getTranslation("action.ready"), () -> orderService.markReady(order.getId())));
            actions.add(cancelButton(order));
        }

        if (order.getStatus() == OrderStatus.READY_FOR_PICKUP) {
            if (order.getAssignedDriver() == null) {
                ComboBox<User> drivers = new ComboBox<>();
                List<User> assignableDrivers = findAssignableDrivers();
                drivers.setItems(assignableDrivers);
                drivers.setItemLabelGenerator(this::driverLabel);
                drivers.setPlaceholder(getTranslation("restaurant.dashboard.driver.placeholder"));
                drivers.setHelperText(assignableDrivers.isEmpty()
                        ? getTranslation("restaurant.dashboard.driver.none")
                        : getTranslation("restaurant.dashboard.driver.helper"));
                drivers.addClassName("pm-driver-select");
                actions.add(drivers);
                actions.add(primaryAction(getTranslation("action.assign"), () -> {
                    if (drivers.getValue() == null) {
                        throw new IllegalStateException(getTranslation("restaurant.orders.chooseDriver"));
                    }
                    deliveryService.assignDriver(order.getId(), drivers.getValue());
                }));
            }
        }

        if (order.getAssignedDriver() != null) {
            Span driver = new Span(getTranslation("restaurant.dashboard.driver.assigned",
                    order.getAssignedDriver().getDisplayName()));
            driver.addClassNames("pm-status-pill", "is-open");
            actions.add(driver);
        }

        deliveryService.findByOrder(order)
                .filter(delivery -> delivery.getStatus() != DeliveryStatus.UNASSIGNED
                        && delivery.getStatus() != DeliveryStatus.REJECTED_BY_DRIVER)
                .ifPresent(delivery -> actions.add(proofSummary(delivery)));

        return actions;
    }

    private Div proofSummary(Delivery delivery) {
        Div proof = new Div();
        proof.addClassName("pm-delivery-proof-summary");
        if (delivery.getProofType() == null) {
            String key = delivery.getStatus() == DeliveryStatus.ON_THE_WAY
                    ? "restaurant.dashboard.proof.pending"
                    : "restaurant.dashboard.proof.awaitingConfirmation";
            proof.add(new Span(getTranslation(key)));
            return proof;
        }
        proof.add(new Span(getTranslation("restaurant.dashboard.proof.method",
                getTranslation("proofType." + delivery.getProofType().name()))));
        if (delivery.getProofImageUrl() != null) {
            com.vaadin.flow.component.html.Image image = new com.vaadin.flow.component.html.Image(
                    delivery.getProofImageUrl(),
                    getTranslation("restaurant.dashboard.proof.imageAlt"));
            image.addClassName("pm-proof-preview");
            proof.add(image);
        }
        return proof;
    }

    private List<User> findAssignableDrivers() {
        return deliveryService.findAvailableAssignableDrivers();
    }

    private String driverLabel(User driver) {
        int activeCount = (int) deliveryService.activeDeliveryCount(driver);
        int limit = deliveryService.getDriverProfile(driver).getActiveDeliveryLimit();
        return getTranslation("restaurant.dashboard.driver.option", driver.getDisplayName(), activeCount, limit);
    }

    private Button primaryAction(String label, Runnable runnable) {
        Button button = new Button(label, event -> runAndRefresh(runnable));
        button.addClassName("pm-primary-action");
        return button;
    }

    private Button dangerAction(String label, Runnable runnable) {
        Button button = new Button(label, event -> runAndRefresh(runnable));
        button.addClassName("pm-danger-action");
        return button;
    }

    private Button cancelButton(CustomerOrder order) {
        Button cancel = new Button(getTranslation("action.cancel"), event -> openCancelDialog(order));
        cancel.addClassName("pm-danger-action");
        return cancel;
    }

    private void openCancelDialog(CustomerOrder order) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("restaurant.dashboard.cancel.title", order.getId()));
        TextArea reason = new TextArea(getTranslation("restaurant.dashboard.cancel.note"));
        reason.setWidthFull();
        reason.setHelperText(getTranslation("restaurant.dashboard.cancel.helper"));
        Button close = new Button(getTranslation("action.close"), event -> dialog.close());
        Button confirm = new Button(getTranslation("restaurant.dashboard.cancel.confirm"), event -> {
            runAndRefresh(() -> orderService.cancelOrder(order.getId(), reason.getValue()));
            dialog.close();
        });
        confirm.addClassName("pm-danger-action");
        dialog.add(reason);
        dialog.getFooter().add(close, confirm);
        dialog.open();
    }

    private Div createMetric(String label, String value, String hint) {
        Div metric = new Div();
        metric.addClassName("pm-metric-card");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("pm-metric-label");
        Span valueSpan = new Span(value);
        valueSpan.addClassName("pm-metric-value");
        Span hintSpan = new Span(hint);
        hintSpan.addClassName("pm-metric-hint");
        metric.add(labelSpan, valueSpan, hintSpan);
        return metric;
    }

    private String money(BigDecimal amount) {
        return amount + " €";
    }

    private Div createEmptyState(String title, String detail) {
        Div empty = new Div();
        empty.addClassName("pm-empty-state");
        empty.add(new H2(title), new Paragraph(detail));
        return empty;
    }

    private String statusClass(OrderStatus status) {
        return switch (status) {
            case PLACED, ACCEPTED, PREPARING, READY_FOR_PICKUP, OUT_FOR_DELIVERY -> "is-open";
            case REJECTED, CANCELLED -> "is-closed";
            default -> "is-todo";
        };
    }

    private long count(List<CustomerOrder> orders, OrderStatus status) {
        return orders.stream().filter(order -> order.getStatus() == status).count();
    }

    private void runAndRefresh(Runnable action) {
        try {
            action.run();
            refresh();
        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void scaffoldPushSubscription() {
        UI ui = UI.getCurrent();
        globalRegistration = orderEventBroadcaster.subscribeToAll(() -> ui.access(this::refresh));
        sessionService.getCurrentUser()
                .flatMap(user -> restaurantService.findForOwner(user).stream().findFirst())
                .ifPresent(restaurant -> registration = orderEventBroadcaster.subscribe(restaurant.getId(), () -> ui.access(this::refresh)));
    }

    private String statusLabel(RestaurantStatus status) {
        return getTranslation("restaurantStatus." + status.name());
    }

    private String statusToneClass(RestaurantStatus status) {
        return switch (status) {
            case OPEN -> "is-open";
            case BUSY -> "is-busy";
            case CLOSED -> "is-closed";
        };
    }
}
