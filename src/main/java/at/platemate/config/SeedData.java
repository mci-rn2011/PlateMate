package at.platemate.config;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import at.platemate.delivery.DriverProfile;
import at.platemate.delivery.DriverProfileRepository;
import at.platemate.delivery.DriverStatus;
import at.platemate.menu.MenuCategory;
import at.platemate.menu.MenuCategoryRepository;
import at.platemate.menu.MenuItem;
import at.platemate.menu.MenuItemRepository;
import at.platemate.order.OrderRepository;
import at.platemate.restaurant.Restaurant;
import at.platemate.restaurant.RestaurantOpeningHours;
import at.platemate.restaurant.RestaurantOpeningHoursRepository;
import at.platemate.restaurant.RestaurantRepository;
import at.platemate.restaurant.RestaurantStatus;
import at.platemate.user.Role;
import at.platemate.user.User;
import at.platemate.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedData implements CommandLineRunner {

    private static final String PLACEHOLDER_LOGO = "/placeholders/restaurant-logo.svg";
    private static final String PLACEHOLDER_BANNER = "/placeholders/restaurant-banner.svg";
    private static final String PLACEHOLDER_ITEM = "/placeholders/menu-item.svg";

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantOpeningHoursRepository openingHoursRepository;
    private final OrderRepository orderRepository;
    private final DriverProfileRepository driverProfileRepository;

    public SeedData(
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            MenuCategoryRepository menuCategoryRepository,
            MenuItemRepository menuItemRepository,
            RestaurantOpeningHoursRepository openingHoursRepository,
            OrderRepository orderRepository,
            DriverProfileRepository driverProfileRepository) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.openingHoursRepository = openingHoursRepository;
        this.orderRepository = orderRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    @Override
    public void run(String... args) {
        removeLegacyDuplicateRestaurant("Noodle Harbor DE");

        ensureUser("Guest", "guest", "guest", Role.CUSTOMER);
        ensureUser("Mia Customer", "mia", "demo", Role.CUSTOMER);
        ensureUser("Noah Customer", "noah", "demo", Role.CUSTOMER);
        User ramenOwner = ensureUser("Sofia Restaurant", "sofia", "demo", Role.RESTAURANT);
        User cafeOwner = ensureUser("Leo Cafe", "leo", "demo", Role.RESTAURANT);
        User mezzeOwner = ensureUser("Maya Mezze", "maya", "demo", Role.RESTAURANT);
        ensureDriverProfile(ensureUser("Emma Driver", "emma", "demo", Role.DRIVER), DriverStatus.AVAILABLE);
        ensureDriverProfile(ensureUser("Lukas Driver", "lukas", "demo", Role.DRIVER), DriverStatus.AVAILABLE);
        ensureDriverProfile(ensureUser("Nina Driver", "nina", "demo", Role.DRIVER), DriverStatus.OFFLINE);

        Restaurant ramen = ensureRestaurant(
                "Noodle Harbor",
                "Warm ramen bowls, crispy gyoza, and quick comfort food.",
                "Ramen",
                "Karlsplatz 7, Vienna",
                RestaurantStatus.OPEN,
                ramenOwner,
                48.2008,
                16.3709);
        Restaurant bakery = ensureRestaurant(
                "Campus Crust",
                "Fresh sandwiches, pastries, and late-study coffee.",
                "Cafe",
                "Favoritenstrasse 22, Vienna",
                RestaurantStatus.BUSY,
                cafeOwner,
                48.1856,
                16.3762);
        Restaurant mezze = ensureRestaurant(
                "Mezze Minute",
                "Fast Mediterranean plates with bright salads and wraps.",
                "Mediterranean",
                "Praterstrasse 18, Vienna",
                RestaurantStatus.CLOSED,
                mezzeOwner,
                48.2154,
                16.3867);
        ensureRestaurantTranslation(ramen, "de", "Noodle Harbor",
                "Warme Ramen-Bowls, knusprige Gyoza und schnelles Comfort Food.", "Ramen");
        ensureRestaurantTranslation(bakery, "de", "Campus Crust",
                "Frische Sandwiches, Gebäck und Kaffee für lange Lerntage.", "Café");
        ensureRestaurantTranslation(mezze, "de", "Mezze Minute",
                "Schnelle mediterrane Teller mit frischen Salaten und Wraps.", "Mediterran");

        seedOpeningHours(ramen, LocalTime.of(11, 0), LocalTime.of(22, 0), true);
        seedOpeningHours(bakery, LocalTime.of(7, 30), LocalTime.of(20, 0), true);
        seedOpeningHours(mezze, LocalTime.of(18, 0), LocalTime.of(2, 0), true);

        MenuCategory ramenBowls = ensureCategory(ramen, "Ramen Bowls", "Steaming noodle bowls with rich broths.", 1);
        MenuCategory ramenSides = ensureCategory(ramen, "Sides", "Small plates to share or add on.", 2);
        MenuCategory ramenDrinks = ensureCategory(ramen, "Drinks", "Cold drinks for a quick refresh.", 3);
        ensureCategoryTranslation(ramenBowls, "de", "Ramen Bowls", "Dampfende Nudelschalen mit kräftiger Brühe.");
        ensureCategoryTranslation(ramenSides, "de", "Beilagen", "Kleine Teller zum Teilen oder Dazubestellen.");
        ensureCategoryTranslation(ramenDrinks, "de", "Getränke", "Kalte Getränke für eine schnelle Erfrischung.");
        ensureItemTranslation(ensureItem(ramen, ramenBowls, "Shoyu Ramen", "Soy broth, noodles, egg, and spring onion.", "12.90", 10),
                "de", "Shoyu Ramen", "Sojabrühe, Nudeln, Ei und Frühlingszwiebel.");
        ensureItemTranslation(ensureItem(ramen, ramenBowls, "Veggie Miso Ramen", "Miso broth with tofu, corn, and greens.", "11.80", 20),
                "de", "Veggie Miso Ramen", "Miso-Brühe mit Tofu, Mais und Gemüse.");
        ensureItemTranslation(ensureItem(ramen, ramenSides, "Chicken Gyoza", "Six pan-fried dumplings with dip.", "5.90", 10),
                "de", "Chicken Gyoza", "Sechs gebratene Teigtaschen mit Dip.");
        ensureItemTranslation(ensureItem(ramen, ramenDrinks, "Yuzu Lemonade", "Sparkling citrus drink.", "3.40", 10),
                "de", "Yuzu-Limonade", "Spritziges Zitrusgetränk.");

        MenuCategory sandwiches = ensureCategory(bakery, "Sandwiches", "Fresh bread, bright fillings, campus-ready.", 1);
        MenuCategory pastries = ensureCategory(bakery, "Pastries", "Sweet bakes from the morning counter.", 2);
        MenuCategory coffee = ensureCategory(bakery, "Coffee & Drinks", "Espresso, iced drinks, and study fuel.", 3);
        ensureCategoryTranslation(sandwiches, "de", "Sandwiches", "Frisches Brot, bunte Füllungen, bereit für den Campus.");
        ensureCategoryTranslation(pastries, "de", "Gebäck", "Süße Backwaren von der Morgentheke.");
        ensureCategoryTranslation(coffee, "de", "Kaffee & Getränke", "Espresso, Iced Drinks und Lernenergie.");
        ensureItem(bakery, sandwiches, "Mozzarella Focaccia", "Tomato, basil, mozzarella, and olive oil.", "7.50", 10);
        ensureItem(bakery, sandwiches, "Avocado Bagel", "Cream cheese, avocado, cucumber, and herbs.", "6.90", 20);
        ensureItem(bakery, pastries, "Cinnamon Roll", "Soft pastry with vanilla glaze.", "3.80", 10);
        ensureItem(bakery, coffee, "Iced Latte", "Double espresso over milk and ice.", "4.20", 10);

        MenuCategory bowls = ensureCategory(mezze, "Bowls", "Layered grains, salads, dips, and crisp toppings.", 1);
        MenuCategory wraps = ensureCategory(mezze, "Wraps", "Handheld Mediterranean favorites.", 2);
        MenuCategory plates = ensureCategory(mezze, "Plates", "Generous plates with warm sides.", 3);
        MenuCategory mezzeDrinks = ensureCategory(mezze, "Drinks", "Cooling classics and house drinks.", 4);
        ensureCategoryTranslation(bowls, "de", "Bowls", "Getreide, Salate, Dips und knackige Toppings.");
        ensureCategoryTranslation(wraps, "de", "Wraps", "Mediterrane Favoriten für die Hand.");
        ensureCategoryTranslation(plates, "de", "Teller", "Großzügige Teller mit warmen Beilagen.");
        ensureCategoryTranslation(mezzeDrinks, "de", "Getränke", "Kühle Klassiker und Hausgetränke.");
        ensureItem(mezze, bowls, "Falafel Bowl", "Falafel, hummus, tabbouleh, and pickles.", "10.90", 10);
        ensureItem(mezze, wraps, "Chicken Shawarma Wrap", "Spiced chicken, garlic sauce, and salad.", "8.90", 10);
        ensureItem(mezze, plates, "Halloumi Plate", "Grilled halloumi with couscous and vegetables.", "12.40", 10);
        ensureItem(mezze, mezzeDrinks, "Mint Ayran", "Chilled yogurt drink with mint.", "3.20", 10);
    }

    private User ensureUser(String displayName, String username, String password, Role role) {
        User user = userRepository.findByDisplayName(displayName)
                .or(() -> userRepository.findByUsernameIgnoreCase(username))
                .orElseGet(() -> new User(displayName, role));
        user.setDisplayName(displayName);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return userRepository.save(user);
    }

    private void ensureDriverProfile(User driver, DriverStatus status) {
        DriverProfile profile = driverProfileRepository.findByUser(driver)
                .orElseGet(() -> new DriverProfile(driver));
        profile.setStatus(status);
        profile.setActiveDeliveryLimit(DriverProfile.DEFAULT_ACTIVE_DELIVERY_LIMIT);
        if (profile.getProfileImageUrl() == null) {
            profile.setProfileImageUrl(PLACEHOLDER_LOGO);
        }
        driverProfileRepository.save(profile);
    }

    private void removeLegacyDuplicateRestaurant(String name) {
        restaurantRepository.findByName(name).ifPresent(restaurant -> {
            if (!orderRepository.findByRestaurantOrderByCreatedAtDesc(restaurant).isEmpty()) {
                return;
            }
            menuItemRepository.deleteAll(menuItemRepository.findByRestaurant(restaurant));
            menuCategoryRepository.deleteAll(menuCategoryRepository.findByRestaurantOrderBySortOrderAscNameAsc(restaurant));
            openingHoursRepository.deleteByRestaurant(restaurant);
            restaurantRepository.delete(restaurant);
        });
    }

    private Restaurant ensureRestaurant(String name, String description, String category, String address,
            RestaurantStatus status, User owner, Double latitude, Double longitude) {
        Restaurant restaurant = restaurantRepository.findByName(name)
                .orElseGet(() -> new Restaurant(name, description, category, address, status != RestaurantStatus.CLOSED,
                        owner));
        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setCategory(category);
        restaurant.setAddress(address);
        restaurant.setStatus(status);
        restaurant.setOwner(owner);
        restaurant.setCoordinates(latitude, longitude);
        if (restaurant.getLogoImageUrl() == null) {
            restaurant.setLogoImageUrl(PLACEHOLDER_LOGO);
        }
        if (restaurant.getBannerImageUrl() == null) {
            restaurant.setBannerImageUrl(PLACEHOLDER_BANNER);
        }
        return restaurantRepository.save(restaurant);
    }

    private MenuCategory ensureCategory(Restaurant restaurant, String name, String description, int sortOrder) {
        MenuCategory category = menuCategoryRepository.findByRestaurantOrderBySortOrderAscNameAsc(restaurant).stream()
                .filter(existing -> existing.getName().equals(name))
                .findFirst()
                .orElseGet(() -> new MenuCategory(restaurant, name, sortOrder));
        category.setName(name);
        category.setDescription(description);
        category.setSortOrder(sortOrder);
        return menuCategoryRepository.save(category);
    }

    private MenuItem ensureItem(Restaurant restaurant, MenuCategory category, String name, String description,
            String price, int sortOrder) {
        MenuItem item = menuItemRepository.findByRestaurantAndName(restaurant, name)
                .orElseGet(() -> new MenuItem(restaurant, name, description, new BigDecimal(price), true));
        item.setCategory(category);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(true);
        item.setSortOrder(sortOrder);
        if (item.getThumbnailImageUrl() == null) {
            item.setThumbnailImageUrl(PLACEHOLDER_ITEM);
        }
        return menuItemRepository.save(item);
    }

    private void ensureRestaurantTranslation(Restaurant restaurant, String locale, String name, String description,
            String category) {
        restaurant.translation(locale).setName(name);
        restaurant.translation(locale).setDescription(description);
        restaurant.translation(locale).setCategory(category);
        restaurantRepository.save(restaurant);
    }

    private void ensureCategoryTranslation(MenuCategory category, String locale, String name, String description) {
        category.translation(locale).setName(name);
        category.translation(locale).setDescription(description);
        menuCategoryRepository.save(category);
    }

    private void ensureItemTranslation(MenuItem item, String locale, String name, String description) {
        item.translation(locale).setName(name);
        item.translation(locale).setDescription(description);
        menuItemRepository.save(item);
    }

    private void seedOpeningHours(Restaurant restaurant, LocalTime opensAt, LocalTime closesAt, boolean closedSunday) {
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean closed = closedSunday && day == DayOfWeek.SUNDAY;
            RestaurantOpeningHours hours = openingHoursRepository.findByRestaurantOrderByDayOfWeekAsc(restaurant)
                    .stream()
                    .filter(existing -> existing.getDayOfWeek() == day)
                    .findFirst()
                    .orElseGet(() -> new RestaurantOpeningHours(restaurant, day, opensAt, closesAt, closed));
            hours.setDayOfWeek(day);
            hours.setOpensAt(closed ? null : opensAt);
            hours.setClosesAt(closed ? null : closesAt);
            hours.setClosed(closed);
            openingHoursRepository.save(hours);
        }
    }
}
