package at.platemate.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import at.platemate.restaurant.Restaurant;
import at.platemate.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    private User assignedDriver;

    private String guestSessionId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String deliveryAddress;
    private String deliveryPostalCode;
    private String deliveryCity;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    @Column(length = 1024)
    private String deliveryAddressNormalized;
    @Column(length = 4096)
    private String routePreviewUrl;
    private String deliveryInstructions;
    private String cancellationNote;

    protected CustomerOrder() {
    }

    public CustomerOrder(User customer, Restaurant restaurant, BigDecimal totalPrice) {
        this(customer, restaurant, totalPrice, null, null, null, null, null, null, null, null);
    }

    public CustomerOrder(
            User customer,
            Restaurant restaurant,
            BigDecimal totalPrice,
            String guestSessionId,
            String contactName,
            String contactPhone,
            String contactEmail,
            String deliveryAddress,
            String deliveryPostalCode,
            String deliveryCity,
            String deliveryInstructions) {
        this.customer = customer;
        this.restaurant = restaurant;
        this.totalPrice = totalPrice;
        this.guestSessionId = blankToNull(guestSessionId);
        this.contactName = blankToNull(contactName);
        this.contactPhone = blankToNull(contactPhone);
        this.contactEmail = blankToNull(contactEmail);
        this.deliveryAddress = blankToNull(deliveryAddress);
        this.deliveryPostalCode = blankToNull(deliveryPostalCode);
        this.deliveryCity = blankToNull(deliveryCity);
        this.deliveryInstructions = blankToNull(deliveryInstructions);
        this.status = OrderStatus.PAYMENT_PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getCustomer() {
        return customer;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getAssignedDriver() {
        return assignedDriver;
    }

    public void setAssignedDriver(User assignedDriver) {
        this.assignedDriver = assignedDriver;
    }

    public String getGuestSessionId() {
        return guestSessionId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getDeliveryPostalCode() {
        return deliveryPostalCode;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public String getFullDeliveryAddress() {
        return String.join(", ", java.util.stream.Stream.of(deliveryAddress, deliveryPostalCode, deliveryCity)
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    public Double getDeliveryLatitude() {
        return deliveryLatitude;
    }

    public void setDeliveryLatitude(Double deliveryLatitude) {
        this.deliveryLatitude = deliveryLatitude;
    }

    public Double getDeliveryLongitude() {
        return deliveryLongitude;
    }

    public void setDeliveryLongitude(Double deliveryLongitude) {
        this.deliveryLongitude = deliveryLongitude;
    }

    public String getDeliveryAddressNormalized() {
        return deliveryAddressNormalized;
    }

    public void setDeliveryAddressNormalized(String deliveryAddressNormalized) {
        this.deliveryAddressNormalized = blankToNull(deliveryAddressNormalized);
    }

    public String getRoutePreviewUrl() {
        return routePreviewUrl;
    }

    public void setRoutePreviewUrl(String routePreviewUrl) {
        this.routePreviewUrl = blankToNull(routePreviewUrl);
    }

    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public String getCancellationNote() {
        return cancellationNote;
    }

    public void setCancellationNote(String cancellationNote) {
        this.cancellationNote = cancellationNote;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
