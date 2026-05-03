package at.platemate.menu;

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
public class MenuCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant;

    private String name;
    private String description;
    private int sortOrder;

    @OneToMany(mappedBy = "menuCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MenuCategoryTranslation> translations = new HashSet<>();

    protected MenuCategory() {
    }

    public MenuCategory(Restaurant restaurant, String name, int sortOrder) {
        this.restaurant = restaurant;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public String getName() {
        return name;
    }

    public String getName(Locale locale) {
        return translated(locale, MenuCategoryTranslation::getName, name);
    }

    public String getDescription() {
        return description;
    }

    public String getDescription(Locale locale) {
        return translated(locale, MenuCategoryTranslation::getDescription, description);
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public MenuCategoryTranslation translation(String locale) {
        String normalized = normalize(locale);
        return translations.stream()
                .filter(translation -> normalized.equals(translation.getLocale()))
                .findFirst()
                .orElseGet(() -> {
                    MenuCategoryTranslation translation = new MenuCategoryTranslation(this, normalized);
                    translations.add(translation);
                    return translation;
                });
    }

    private String translated(Locale locale, Function<MenuCategoryTranslation, String> getter, String fallback) {
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
