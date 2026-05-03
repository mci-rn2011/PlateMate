package at.platemate.menu;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import at.platemate.restaurant.Restaurant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.EAGER)
    private MenuCategory category;

    private String name;
    private String description;
    private BigDecimal price;
    private boolean available;
    private String thumbnailImageUrl;
    private Integer sortOrder;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuItemTranslation> translations = new HashSet<>();

    protected MenuItem() {
    }

    public MenuItem(Restaurant restaurant, String name, String description, BigDecimal price, boolean available) {
        this.restaurant = restaurant;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public MenuCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getName(Locale locale) {
        return translated(locale, MenuItemTranslation::getName, name);
    }

    public String getDescription() {
        return description;
    }

    public String getDescription(Locale locale) {
        return translated(locale, MenuItemTranslation::getDescription, description);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getThumbnailImageUrl() {
        return thumbnailImageUrl;
    }

    public int getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public void setCategory(MenuCategory category) {
        this.category = category;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setThumbnailImageUrl(String thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public MenuItemTranslation translation(String locale) {
        String normalized = normalize(locale);
        return translations.stream()
                .filter(translation -> normalized.equals(translation.getLocale()))
                .findFirst()
                .orElseGet(() -> {
                    MenuItemTranslation translation = new MenuItemTranslation(this, normalized);
                    translations.add(translation);
                    return translation;
                });
    }

    private String translated(Locale locale, Function<MenuItemTranslation, String> getter, String fallback) {
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
