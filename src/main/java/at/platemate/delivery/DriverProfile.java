package at.platemate.delivery;

import at.platemate.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class DriverProfile {

    public static final int DEFAULT_ACTIVE_DELIVERY_LIMIT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private DriverStatus status = DriverStatus.OFFLINE;

    private int activeDeliveryLimit = DEFAULT_ACTIVE_DELIVERY_LIMIT;

    protected DriverProfile() {
    }

    public DriverProfile(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public DriverStatus getStatus() {
        return status == null ? DriverStatus.OFFLINE : status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status == null ? DriverStatus.OFFLINE : status;
    }

    public int getActiveDeliveryLimit() {
        return activeDeliveryLimit <= 0 ? DEFAULT_ACTIVE_DELIVERY_LIMIT : activeDeliveryLimit;
    }

    public void setActiveDeliveryLimit(int activeDeliveryLimit) {
        this.activeDeliveryLimit = activeDeliveryLimit <= 0 ? DEFAULT_ACTIVE_DELIVERY_LIMIT : activeDeliveryLimit;
    }
}
