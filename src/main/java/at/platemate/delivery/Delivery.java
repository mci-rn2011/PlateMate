package at.platemate.delivery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import at.platemate.order.CustomerOrder;
import at.platemate.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    private User driver;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Column(precision = 8, scale = 2)
    private BigDecimal distanceKm;

    private Integer estimatedMinutes;

    @Column(precision = 8, scale = 2)
    private BigDecimal deliveryFee;

    @Enumerated(EnumType.STRING)
    private ProofType proofType;

    private String proofImageUrl;
    private String confirmationToken;
    @Column(length = 4096)
    private String routePreviewUrl;
    private LocalDateTime confirmedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;

    protected Delivery() {
    }

    public Delivery(CustomerOrder order, User driver) {
        this.order = order;
        this.driver = driver;
        this.status = DeliveryStatus.ASSIGNED;
        this.assignedAt = LocalDateTime.now();
        this.confirmationToken = UUID.randomUUID().toString();
    }

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
        this.assignedAt = LocalDateTime.now();
        if (this.confirmationToken == null || this.confirmationToken.isBlank()) {
            this.confirmationToken = UUID.randomUUID().toString();
        }
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
        if (status == DeliveryStatus.PICKED_UP && this.pickedUpAt == null) {
            this.pickedUpAt = LocalDateTime.now();
        }
        if (status == DeliveryStatus.DELIVERED) {
            this.deliveredAt = LocalDateTime.now();
        }
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public ProofType getProofType() {
        return proofType;
    }

    public void setProofType(ProofType proofType) {
        this.proofType = proofType;
    }

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public String getConfirmationCode() {
        if (confirmationToken == null || confirmationToken.isBlank()) {
            return "";
        }
        return confirmationToken.replace("-", "").substring(0, 6).toUpperCase();
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public String getRoutePreviewUrl() {
        return routePreviewUrl;
    }

    public void setRoutePreviewUrl(String routePreviewUrl) {
        this.routePreviewUrl = routePreviewUrl;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void confirmQrProof() {
        this.proofType = ProofType.QR_CODE;
        this.confirmedAt = LocalDateTime.now();
    }

    public void confirmPhotoProof(String proofImageUrl) {
        this.proofType = ProofType.PHOTO;
        this.proofImageUrl = proofImageUrl;
        this.confirmedAt = LocalDateTime.now();
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
}
