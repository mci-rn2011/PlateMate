package at.platemate.restaurant;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import at.platemate.user.User;
import jakarta.persistence.CascadeType;
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
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String category;
    private String address;
    private boolean open;
    private String logoImageUrl;
    private String bannerImageUrl;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private RestaurantStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    private User owner;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RestaurantTranslation> translations = new HashSet<>();

    protected Restaurant() {
    }

    public Restaurant(String name, String description, String category, String address, boolean open, User owner) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.address = address;
        this.open = open;
        this.status = open ? RestaurantStatus.OPEN : RestaurantStatus.CLOSED;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getName(Locale locale) {
        return translated(locale, RestaurantTranslation::getName, name);
    }

    public String getDescription() {
        return description;
    }

    public String getDescription(Locale locale) {
        return translated(locale, RestaurantTranslation::getDescription, description);
    }

    public String getCategory() {
        return category;
    }

    public String getCategory(Locale locale) {
        return translated(locale, RestaurantTranslation::getCategory, category);
    }

    public String getAddress() {
        return address;
    }

    public boolean isOpen() {
        RestaurantStatus currentStatus = getStatus();
        return open && (currentStatus == RestaurantStatus.OPEN || currentStatus == RestaurantStatus.BUSY);
    }

    public String getLogoImageUrl() {
        return logoImageUrl;
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public RestaurantStatus getStatus() {
        return status == null ? (open ? RestaurantStatus.OPEN : RestaurantStatus.CLOSED) : status;
    }

    public User getOwner() {
        return owner;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setOpen(boolean open) {
        this.open = open;
        this.status = open ? RestaurantStatus.OPEN : RestaurantStatus.CLOSED;
    }

    public void setLogoImageUrl(String logoImageUrl) {
        this.logoImageUrl = logoImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    public void setCoordinates(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setStatus(RestaurantStatus status) {
        this.status = status;
        this.open = status == RestaurantStatus.OPEN || status == RestaurantStatus.BUSY;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public RestaurantTranslation translation(String locale) {
        String normalized = normalize(locale);
        return translations.stream()
                .filter(translation -> normalized.equals(translation.getLocale()))
                .findFirst()
                .orElseGet(() -> {
                    RestaurantTranslation translation = new RestaurantTranslation(this, normalized);
                    translations.add(translation);
                    return translation;
                });
    }

    private String translated(Locale locale, Function<RestaurantTranslation, String> getter, String fallback) {
        String language = locale == null ? "en" : locale.getLanguage();
        return translations.stream()
                .filter(translation -> normalize(language).equals(translation.getLocale()))
                .map(getter)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .or(() -> translations.stream()
                        .filter(translation -> "en".equals(translation.getLocale()))
                        .map(getter)
                        .filter(value -> value != null && !value.isBlank())
                        .findFirst())
                .orElse(fallback);
    }

    private String normalize(String locale) {
        return locale == null || locale.isBlank() ? "en" : Locale.forLanguageTag(locale).getLanguage();
    }
}
