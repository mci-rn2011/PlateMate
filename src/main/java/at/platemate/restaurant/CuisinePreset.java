package at.platemate.restaurant;

import java.util.Arrays;
import java.util.Locale;

public enum CuisinePreset {
    RAMEN("Ramen", "Ramen", "🍜"),
    CAFE("Cafe", "Café", "☕"),
    MEDITERRANEAN("Mediterranean", "Mediterran", "🥙"),
    PIZZA("Pizza", "Pizza", "🍕"),
    BURGER("Burger", "Burger", "🍔"),
    SUSHI("Sushi", "Sushi", "🍣"),
    VEGAN("Vegan", "Vegan", "🥗"),
    DESSERT("Dessert", "Dessert", "🍰"),
    KEBAB("Kebab", "Kebab", "🥙"),
    BOWLS("Bowls", "Bowls", "🥣");

    private final String englishName;
    private final String germanName;
    private final String emoji;

    CuisinePreset(String englishName, String germanName, String emoji) {
        this.englishName = englishName;
        this.germanName = germanName;
        this.emoji = emoji;
    }

    public String englishName() {
        return englishName;
    }

    public String germanName() {
        return germanName;
    }

    public String displayName(Locale locale) {
        String language = locale == null ? "en" : locale.getLanguage();
        return emoji + " " + ("de".equals(language) ? germanName : englishName);
    }

    public static CuisinePreset fromCategory(String category) {
        if (category == null || category.isBlank()) {
            return RAMEN;
        }
        return Arrays.stream(values())
                .filter(preset -> preset.englishName.equalsIgnoreCase(category)
                        || preset.germanName.equalsIgnoreCase(category))
                .findFirst()
                .orElse(RAMEN);
    }
}
