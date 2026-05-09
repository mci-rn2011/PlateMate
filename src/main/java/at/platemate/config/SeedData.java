package at.platemate.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import at.platemate.restaurant.RestaurantRepository;
import at.platemate.restaurant.RestaurantOpeningHoursRepository;
import at.platemate.restaurant.RestaurantStatus;
import at.platemate.user.Role;
import at.platemate.user.User;
import at.platemate.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements CommandLineRunner {

    private static final String PLACEHOLDER_LOGO = "/placeholders/restaurant-logo.svg";
    private static final String PLACEHOLDER_BANNER = "/placeholders/restaurant-banner.svg";
    private static final String PLACEHOLDER_ITEM = "/placeholders/menu-item.svg";
    private static final Map<String, String> SEED_BANNER_IMAGE_URLS = loadSeedImageMap(
            "seed/restaurant-banners.tsv", 1);
    private static final Map<String, String> SEED_MENU_ITEM_IMAGE_URLS = loadSeedImageMap(
            "seed/menu-item-images.tsv", 2);
    private static final Map<String, String> SEED_LOGO_IMAGE_URLS = loadSeedImageMap(
            "seed/restaurant-logos.tsv", 1);

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
    @Transactional
    public void run(String... args) {
        removeLegacyDuplicateRestaurant("Noodle Harbor DE");
        removeLegacyRestaurantIfUnused("Noodle Harbor");
        removeLegacyRestaurantIfUnused("Campus Crust");
        removeLegacyRestaurantIfUnused("Mezze Minute");
        removeLegacyRestaurantIfUnused("Slice Society");

        ensureUser("Guest", "guest", "guest", Role.CUSTOMER);
        seedCustomerUsers();
        ensureDriverProfile(ensureUser("Emma Driver", "emma", "demo", Role.DRIVER), DriverStatus.AVAILABLE);
        ensureDriverProfile(ensureUser("Lukas Driver", "lukas", "demo", Role.DRIVER), DriverStatus.AVAILABLE);
        ensureDriverProfile(ensureUser("Nina Driver", "nina", "demo", Role.DRIVER), DriverStatus.OFFLINE);
        seedInnsbruckRestaurants();
    }

    private void seedCustomerUsers() {
        ensureCustomerUser("Nicolas", "nicolas", "Maria-Theresien-Straße 18", "6020", "Innsbruck");
        ensureCustomerUser("Emilia", "emilia", "Innrain 52", "6020", "Innsbruck");
        ensureCustomerUser("Marvin", "marvin", "Pradler Straße 34", "6020", "Innsbruck");
        ensureCustomerUser("Alexander", "alexander", "Höttinger Au 28", "6020", "Innsbruck");
        ensureCustomerUser("Andrej", "andrej", "Amraser Straße 76", "6020", "Innsbruck");
    }

    private void seedInnsbruckRestaurants() {
        for (RestaurantSeed seed : innsbruckRestaurantSeeds()) {
            User owner = ensureUser(seed.name() + " Owner", ownerUsername(seed.name()), "demo", Role.RESTAURANT);
            Restaurant restaurant = ensureRestaurant(seed.name(), seed.description(), seed.category(), seed.address(),
                    seed.status(), owner, seed.latitude(), seed.longitude());
            ensureRestaurantTranslation(restaurant, "de", seed.name(), seed.description(), seed.category());
            seedOpeningHours(restaurant, seed.opensAt(), seed.closesAt(), true);
            seedDemoMenu(restaurant, seed.category());
        }
    }

    private List<RestaurantSeed> innsbruckRestaurantSeeds() {
        return List.of(
                new RestaurantSeed("die Wilderin", "Regional Austrian plates with a modern market-kitchen feel.", "Austrian",
                        "Seilergasse 5, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2683, 11.3936, LocalTime.of(11, 30), LocalTime.of(22, 0)),
                new RestaurantSeed("Gasthof Weisses Rössl", "Classic Tyrolean comfort food from the old town.", "Austrian",
                        "Kiebachgasse 8, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2687, 11.3928, LocalTime.of(11, 0), LocalTime.of(22, 0)),
                new RestaurantSeed("Goldener Adler Restaurant", "Austrian and international dishes in a historic setting.", "Austrian",
                        "Herzog-Friedrich-Straße 6, 6020 Innsbruck", RestaurantStatus.BUSY, 47.2685, 11.3937, LocalTime.of(11, 30), LocalTime.of(21, 30)),
                new RestaurantSeed("Stiftskeller Innsbruck", "Traditional Austrian kitchen with beer-hall energy.", "Austrian",
                        "Stiftgasse 1, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2686, 11.3952, LocalTime.of(11, 0), LocalTime.of(23, 0)),
                new RestaurantSeed("Goldenes Dachl Restaurant", "Tyrolean classics right by the landmark old town.", "Austrian",
                        "Hofgasse 1, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2688, 11.3939, LocalTime.of(10, 30), LocalTime.of(22, 0)),
                new RestaurantSeed("Wirtshaus Schöneck", "Austrian fusion plates from above the city.", "Austrian",
                        "Weiherburggasse 6, 6020 Innsbruck", RestaurantStatus.CLOSED, 47.2767, 11.3964, LocalTime.of(12, 0), LocalTime.of(22, 0)),
                new RestaurantSeed("Fischerhäusl", "Historic Austrian dining with hearty classics.", "Austrian",
                        "Herrengasse 8, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2691, 11.3948, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                new RestaurantSeed("Akropolis", "Greek and Mediterranean plates with seafood and grilled favorites.", "Greek",
                        "Innrain 13, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2667, 11.3924, LocalTime.of(11, 30), LocalTime.of(22, 30)),
                new RestaurantSeed("Due Sicilie", "Southern Italian trattoria dishes and pizza.", "Italian",
                        "Höttinger Gasse 15, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2700, 11.3915, LocalTime.of(11, 30), LocalTime.of(22, 30)),
                new RestaurantSeed("L'Osteria Innsbruck", "Italian pizza and pasta for quick group orders.", "Pizza",
                        "Erlerstraße 17, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2661, 11.3977, LocalTime.of(11, 0), LocalTime.of(23, 0)),
                new RestaurantSeed("Vapiano Innsbruck Triumphpforte", "Fresh pasta, pizza, and salads near Triumphpforte.", "Italian",
                        "Leopoldstraße 1, 6020 Innsbruck", RestaurantStatus.BUSY, 47.2619, 11.3946, LocalTime.of(11, 0), LocalTime.of(22, 0)),
                new RestaurantSeed("Indisches Restaurant Rama", "Indian curries, rice dishes, and vegetarian plates.", "Indian",
                        "Innstraße 81, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2744, 11.3928, LocalTime.of(11, 30), LocalTime.of(22, 0)),
                new RestaurantSeed("TAJ Indisches Restaurant", "North Indian comfort food with rich sauces.", "Indian",
                        "Museumstraße 28, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2667, 11.3996, LocalTime.of(11, 30), LocalTime.of(22, 0)),
                new RestaurantSeed("Teppan Wok", "Asian wok dishes, sushi, and warm bowls.", "Sushi",
                        "Bürgerstraße 2, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2644, 11.3938, LocalTime.of(11, 0), LocalTime.of(22, 0)),
                new RestaurantSeed("Sensei", "Sushi and Asian fusion plates in the city center.", "Sushi",
                        "Maria-Theresien-Straße 11, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2664, 11.3943, LocalTime.of(12, 0), LocalTime.of(22, 0)),
                new RestaurantSeed("Mexico Arriba", "Mexican tapas, tacos, and colorful dinner plates.", "Mexican",
                        "Innrain 2, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2671, 11.3929, LocalTime.of(18, 0), LocalTime.of(22, 0)),
                new RestaurantSeed("Himal Nepali Kitchen", "Nepalese momos, curries, and warming rice dishes.", "Nepalese",
                        "Universitätsstraße 13, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2685, 11.3988, LocalTime.of(11, 30), LocalTime.of(22, 0)),
                new RestaurantSeed("Gaia Cuisine", "International vegan-friendly fusion with a modern touch.", "Vegan",
                        "Höttinger Gasse 6, 6020 Innsbruck", RestaurantStatus.BUSY, 47.2695, 11.3919, LocalTime.of(17, 0), LocalTime.of(23, 0)),
                new RestaurantSeed("Kaukas Restaurant", "Caucasian and Middle Eastern plates with grilled specialties.", "Middle Eastern",
                        "Innstraße 19, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2710, 11.3939, LocalTime.of(11, 30), LocalTime.of(22, 0)),
                new RestaurantSeed("Die Brennerei", "Filipino-inspired comfort food and shareable plates.", "Filipino",
                        "Philippine-Welser-Straße 88, 6020 Innsbruck", RestaurantStatus.OPEN, 47.2550, 11.4205, LocalTime.of(11, 30), LocalTime.of(22, 0)));
    }

    private void seedDemoMenu(Restaurant restaurant, String category) {
        String cuisine = category == null ? "" : category.toLowerCase(Locale.ROOT);
        if (cuisine.contains("austrian")) {
            seedMenu(restaurant, List.of(
                    category("Tyrolean Classics", "Schnitzel, dumplings, and Alpine comfort.", item("Wiener Schnitzel", "Crispy veal schnitzel with parsley potatoes.", "15.90"), item("Käsespätzle", "Cheese noodles with roasted onions.", "12.40")),
                    category("Mountain Bowls", "Warm bowls inspired by Tyrolean ingredients.", item("Gröstl Bowl", "Potato gröstl with egg and herb salad.", "11.90")),
                    category("Sides & Sweets", "Small extras and sweet finishes.", item("Apfelstrudel", "Apple strudel with vanilla sauce.", "5.40"), item("Mixed Salad", "Leaf salad with pumpkin seed dressing.", "4.60"))));
        } else if (cuisine.contains("italian") || cuisine.contains("pizza")) {
            seedMenu(restaurant, List.of(
                    category("Pizza", "Stone-style pizza favorites.", item("Pizza Margherita", "Tomato, mozzarella, basil.", "9.90"), item("Pizza Diavola", "Spicy salami, chili, mozzarella.", "12.40")),
                    category("Pasta", "Fresh pasta and sauces.", item("Penne Arrabbiata", "Tomato, garlic, chili, parsley.", "10.80")),
                    category("Antipasti", "Italian starters and sides.", item("Caprese", "Tomato, mozzarella, basil oil.", "7.50"), item("Tiramisu", "Coffee mascarpone dessert.", "5.90"))));
        } else if (cuisine.contains("indian") || cuisine.contains("nepalese")) {
            seedMenu(restaurant, List.of(
                    category("Curries", "Rich sauces and warming spices.", item("Butter Chicken", "Creamy tomato curry with rice.", "13.90"), item("Dal Tadka", "Yellow lentils with cumin and garlic.", "10.90")),
                    category("Momos & Starters", "Small plates for sharing.", item("Vegetable Momos", "Steamed dumplings with tomato chutney.", "8.40")),
                    category("Rice & Bread", "Fresh sides from the tandoor.", item("Garlic Naan", "Soft naan with garlic butter.", "3.90"), item("Mango Lassi", "Chilled yoghurt mango drink.", "3.80"))));
        } else if (cuisine.contains("sushi")) {
            seedMenu(restaurant, List.of(
                    category("Sushi Sets", "Fresh rolls and nigiri boxes.", item("Salmon Maki Set", "Eight salmon maki with soy and ginger.", "9.90"), item("Veggie Sushi Box", "Avocado, cucumber, and tofu rolls.", "10.40")),
                    category("Wok Bowls", "Hot rice and noodle bowls.", item("Teriyaki Chicken Bowl", "Chicken, vegetables, rice, teriyaki.", "12.90")),
                    category("Sides", "Small Asian starters.", item("Edamame", "Steamed soy beans with sea salt.", "4.20"), item("Miso Soup", "Miso broth with tofu and wakame.", "3.90"))));
        } else if (cuisine.contains("mexican")) {
            seedMenu(restaurant, List.of(
                    category("Tacos", "Soft tacos with fresh toppings.", item("Chicken Tacos", "Three tacos with salsa verde.", "10.90"), item("Veggie Tacos", "Beans, corn, avocado, lime.", "9.90")),
                    category("Bowls", "Rice bowls with bright sauces.", item("Burrito Bowl", "Rice, beans, pico de gallo, sour cream.", "11.80")),
                    category("Sides", "Crispy extras and dips.", item("Nachos & Guacamole", "Corn chips with guacamole.", "6.90"), item("Churros", "Cinnamon churros with chocolate dip.", "5.40"))));
        } else if (cuisine.contains("greek") || cuisine.contains("middle")) {
            seedMenu(restaurant, List.of(
                    category("Grill", "Grilled plates with warm sides.", item("Chicken Souvlaki", "Skewers with pita and tzatziki.", "12.90"), item("Köfte Plate", "Spiced meatballs with bulgur.", "13.40")),
                    category("Bowls & Wraps", "Fresh handhelds and bowls.", item("Falafel Wrap", "Falafel, hummus, herbs, pickles.", "8.90")),
                    category("Mezze", "Small plates for sharing.", item("Hummus Plate", "Hummus with pita and olive oil.", "5.90"), item("Baklava", "Sweet pastry with nuts and syrup.", "4.80"))));
        } else if (cuisine.contains("vegan")) {
            seedMenu(restaurant, List.of(
                    category("Plant Bowls", "Fresh bowls with seasonal produce.", item("Green Gaia Bowl", "Quinoa, avocado, greens, tahini.", "12.90"), item("Miso Mushroom Bowl", "Rice, mushrooms, miso glaze.", "13.40")),
                    category("Small Plates", "Colorful vegan starters.", item("Crispy Cauliflower", "Cauliflower bites with lime dip.", "7.40")),
                    category("Sweets & Drinks", "Plant-based finishes.", item("Chocolate Chia Cup", "Coconut chia and dark chocolate.", "5.20"), item("Ginger Lemonade", "House lemonade with ginger.", "3.90"))));
        } else if (cuisine.contains("filipino")) {
            seedMenu(restaurant, List.of(
                    category("Rice Plates", "Filipino-inspired comfort plates.", item("Chicken Adobo", "Soy-vinegar chicken with rice.", "12.90"), item("Pork Tocino Bowl", "Sweet pork, garlic rice, egg.", "13.40")),
                    category("Noodles", "Warm noodle classics.", item("Pancit Canton", "Stir-fried noodles with vegetables.", "10.90")),
                    category("Sides & Sweets", "Crunchy snacks and desserts.", item("Lumpia", "Crispy rolls with sweet chili.", "6.40"), item("Halo-Halo Cup", "Iced dessert with fruit and cream.", "5.80"))));
        } else {
            seedMenu(restaurant, List.of(
                    category("Favorites", "House favorites for quick orders.", item("Signature Bowl", "Rice, vegetables, house sauce.", "11.90"), item("Loaded Wrap", "Fresh wrap with crisp salad.", "9.90")),
                    category("Small Plates", "Easy sides for sharing.", item("Crispy Bites", "Crunchy bites with dip.", "5.90")),
                    category("Drinks", "Cold drinks and simple sweets.", item("House Lemonade", "Sparkling citrus lemonade.", "3.80"), item("Chocolate Cake", "Small chocolate dessert.", "4.90"))));
        }
    }

    private void seedMenu(Restaurant restaurant, List<CategorySeed> categories) {
        int categorySort = 1;
        for (CategorySeed categorySeed : categories) {
            MenuCategory category = ensureCategory(restaurant, categorySeed.name(), categorySeed.description(), categorySort++);
            int itemSort = 10;
            for (ItemSeed itemSeed : categorySeed.items()) {
                ensureItem(restaurant, category, itemSeed.name(), itemSeed.description(), itemSeed.price(), itemSort);
                itemSort += 10;
            }
        }
    }

    private CategorySeed category(String name, String description, ItemSeed... items) {
        return new CategorySeed(name, description, List.of(items));
    }

    private ItemSeed item(String name, String description, String price) {
        return new ItemSeed(name, description, price);
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

    private User ensureCustomerUser(String displayName, String username, String address, String postalCode, String city) {
        User user = ensureUser(displayName, username, "demo", Role.CUSTOMER);
        user.setAddress(address);
        user.setPostalCode(postalCode);
        user.setCity(city);
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
        removeLegacyRestaurantIfUnused(name);
    }

    private void removeLegacyRestaurantIfUnused(String name) {
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
        if (shouldReplaceSeedLogo(restaurant.getLogoImageUrl())) {
            restaurant.setLogoImageUrl(SEED_LOGO_IMAGE_URLS.getOrDefault(name, PLACEHOLDER_LOGO));
        }
        String currentBanner = restaurant.getBannerImageUrl();
        if (shouldReplaceSeedBanner(currentBanner)) {
            restaurant.setBannerImageUrl(SEED_BANNER_IMAGE_URLS.getOrDefault(name, PLACEHOLDER_BANNER));
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
        String seedImageUrl = SEED_MENU_ITEM_IMAGE_URLS.get(seedMenuItemKey(restaurant.getName(), name));
        if (seedImageUrl != null && shouldReplaceSeedItemImage(item.getThumbnailImageUrl())) {
            item.setThumbnailImageUrl(seedImageUrl);
        } else if (item.getThumbnailImageUrl() == null) {
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

    private String ownerUsername(String restaurantName) {
        String slug = restaurantName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.|\\.$", "");
        return "owner." + slug;
    }

    private boolean shouldReplaceSeedBanner(String bannerImageUrl) {
        return bannerImageUrl == null
                || bannerImageUrl.isBlank()
                || PLACEHOLDER_BANNER.equals(bannerImageUrl)
                || bannerImageUrl.startsWith("/seed-banners/");
    }

    private boolean shouldReplaceSeedLogo(String logoImageUrl) {
        return logoImageUrl == null
                || logoImageUrl.isBlank()
                || PLACEHOLDER_LOGO.equals(logoImageUrl)
                || logoImageUrl.startsWith("/demo/logos/");
    }

    private boolean shouldReplaceSeedItemImage(String imageUrl) {
        return imageUrl == null
                || imageUrl.isBlank()
                || PLACEHOLDER_ITEM.equals(imageUrl)
                || imageUrl.startsWith("https://images.pexels.com/");
    }

    private static String seedMenuItemKey(String restaurantName, String itemName) {
        return restaurantName + "\t" + itemName;
    }

    private static Map<String, String> loadSeedImageMap(String resourceName, int keyColumns) {
        try (var stream = SeedData.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                return Map.of();
            }
            Map<String, String> images = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    if (keyColumns == 1 && parts.length >= 2) {
                        images.put(parts[0], parts[1]);
                    } else if (keyColumns == 2 && parts.length >= 3) {
                        images.put(seedMenuItemKey(parts[0], parts[1]), parts[2]);
                    }
                }
            }
            return Map.copyOf(images);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load seed image map " + resourceName, exception);
        }
    }

    private record RestaurantSeed(
            String name,
            String description,
            String category,
            String address,
            RestaurantStatus status,
            double latitude,
            double longitude,
            LocalTime opensAt,
            LocalTime closesAt) {
    }

    private record CategorySeed(String name, String description, List<ItemSeed> items) {
    }

    private record ItemSeed(String name, String description, String price) {
    }
}
