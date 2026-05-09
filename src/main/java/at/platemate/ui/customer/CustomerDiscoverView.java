package at.platemate.ui.customer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import at.platemate.auth.MockSessionService;
import at.platemate.cart.CartService;
import at.platemate.delivery.GeocodedLocation;
import at.platemate.delivery.LocationService;
import at.platemate.restaurant.Restaurant;
import at.platemate.restaurant.RestaurantEventBroadcaster;
import at.platemate.restaurant.RestaurantOpeningHours;
import at.platemate.restaurant.RestaurantService;
import at.platemate.restaurant.RestaurantStatus;
import at.platemate.ui.layout.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteParameters;

@Route(value = "customer/discover", layout = MainLayout.class)
@RouteAlias(value = "customer/restaurants", layout = MainLayout.class)
@PageTitle("Discover | PlateMate")
public class CustomerDiscoverView extends VerticalLayout {

    private static final String PLACEHOLDER_BANNER = "placeholders/restaurant-banner.svg";
    private static final String PLACEHOLDER_LOGO = "placeholders/restaurant-logo.svg";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final RestaurantService restaurantService;
    private final CartService cartService;
    private final RestaurantEventBroadcaster restaurantEventBroadcaster;
    private final LocationService locationService;
    private final MockSessionService sessionService;
    private final TextField location = new TextField();
    private final TextField search = new TextField();
    private final Span locationHint = new Span();
    private final Div chipScroller = new Div();
    private final Div restaurantGrid = new Div();
    private final Set<String> activeCuisineFilters = new LinkedHashSet<>();
    private Double customerLatitude;
    private Double customerLongitude;
    private RestaurantEventBroadcaster.Registration registration;

    public CustomerDiscoverView(
            RestaurantService restaurantService,
            CartService cartService,
            RestaurantEventBroadcaster restaurantEventBroadcaster,
            LocationService locationService,
            MockSessionService sessionService) {
        this.restaurantService = restaurantService;
        this.cartService = cartService;
        this.restaurantEventBroadcaster = restaurantEventBroadcaster;
        this.locationService = locationService;
        this.sessionService = sessionService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames("pm-customer-page", "pm-discover-page");

        add(createHero(), createFilterPanel(), restaurantGrid);
        restoreSelectedLocation();
        refreshRestaurants();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        registration = restaurantEventBroadcaster.subscribe(() -> ui.access(this::refreshRestaurants));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    private Div createHero() {
        Div hero = new Div();
        hero.addClassNames("pm-customer-hero", "pm-discover-hero");

        Image logo = new Image("brand/platemate-logo-wordmark.png", "PlateMate");
        logo.addClassName("pm-discover-logo");
        H1 title = new H1(getTranslation("customer.discover.title"));
        Paragraph intro = new Paragraph(getTranslation("customer.discover.intro"));

        location.setPlaceholder(getTranslation("customer.discover.location.placeholder"));
        location.setClearButtonVisible(true);
        location.addClassName("pm-location-input");

        Button locate = new Button();
        locate.addClickListener(event -> requestBrowserLocation());
        locate.setIcon(VaadinIcon.MAP_MARKER.create());
        locate.addClassNames("pm-location-icon-button");
        locate.getElement().setAttribute("aria-label", getTranslation("customer.discover.location.use"));

        Button searchButton = new Button();
        searchButton.addClickListener(event -> confirmManualLocation());
        searchButton.setIcon(VaadinIcon.SEARCH.create());
        searchButton.addClassNames("pm-primary-action", "pm-location-search-button");
        searchButton.getElement().setAttribute("aria-label", getTranslation("customer.discover.location.search"));

        Div locationRow = new Div(locate, location, searchButton);
        locationRow.addClassName("pm-location-row");
        locationHint.addClassName("pm-location-hint");
        hero.add(logo, title, intro, locationRow, locationHint);
        return hero;
    }

    private void confirmManualLocation() {
        String value = location.getValue() == null ? "" : location.getValue().trim();
        if (value.isBlank()) {
            customerLatitude = null;
            customerLongitude = null;
            sessionService.clearSelectedDeliveryLocation();
            locationHint.setText("");
            refreshRestaurants();
            return;
        }
        List<GeocodedLocation> suggestions = locationService.searchForwardGeocode(value, 5);
        if (suggestions.isEmpty()) {
            Notification.show(getTranslation("customer.discover.location.notFound"));
            return;
        }
        if (suggestions.size() == 1) {
            applyLocation(suggestions.get(0), getTranslation("customer.discover.location.verified"));
            return;
        }
        showLocationPicker(suggestions);
    }

    private void showLocationPicker(List<GeocodedLocation> suggestions) {
        Dialog dialog = new Dialog();
        dialog.addClassName("pm-location-picker-dialog");
        dialog.setHeaderTitle(getTranslation("customer.discover.location.choose"));
        Div list = new Div();
        list.addClassName("pm-location-suggestion-list");
        suggestions.forEach(suggestion -> {
            Button option = new Button(suggestion.normalizedAddress(), event -> {
                applyLocation(suggestion, getTranslation("customer.discover.location.verified"));
                dialog.close();
            });
            option.addClassName("pm-location-suggestion");
            list.add(option);
        });
        dialog.add(list);
        dialog.getFooter().add(new Button(getTranslation("action.close"), event -> dialog.close()));
        dialog.open();
    }

    private void applyLocation(GeocodedLocation geocodedLocation, String hint) {
        customerLatitude = geocodedLocation.coordinates().latitude();
        customerLongitude = geocodedLocation.coordinates().longitude();
        sessionService.setSelectedDeliveryLocation(geocodedLocation);
        location.setValue(geocodedLocation.normalizedAddress());
        locationHint.setText(hint);
        refreshRestaurants();
    }

    private void restoreSelectedLocation() {
        sessionService.getSelectedDeliveryLocation().ifPresent(selected -> {
            customerLatitude = selected.coordinates().latitude();
            customerLongitude = selected.coordinates().longitude();
            location.setValue(selected.normalizedAddress());
            locationHint.setText(getTranslation("customer.discover.location.verified"));
        });
        if (customerLatitude == null) {
            sessionService.getCurrentUser()
                    .filter(user -> user.getAddress() != null && !user.getAddress().isBlank())
                    .flatMap(user -> locationService.forwardGeocode(String.join(", ",
                            java.util.stream.Stream.of(user.getAddress(), user.getPostalCode(), user.getCity())
                                    .filter(value -> value != null && !value.isBlank())
                                    .toList())))
                    .ifPresent(selected -> {
                        customerLatitude = selected.coordinates().latitude();
                        customerLongitude = selected.coordinates().longitude();
                        sessionService.setSelectedDeliveryLocation(selected);
                        location.setValue(selected.normalizedAddress());
                        locationHint.setText(getTranslation("customer.discover.location.verified"));
                    });
        }
    }

    private Div createFilterPanel() {
        Div panel = new Div();
        panel.addClassName("pm-discover-filters");

        search.setPlaceholder(getTranslation("customer.discover.search.placeholder"));
        search.setClearButtonVisible(true);
        search.addValueChangeListener(event -> refreshRestaurants());
        search.addClassName("pm-discover-search");

        Button left = new Button("<", event -> scrollChips(-1));
        Button right = new Button(">", event -> scrollChips(1));
        left.addClassName("pm-chip-arrow");
        right.addClassName("pm-chip-arrow");

        chipScroller.addClassName("pm-chip-scroller");
        cuisineTypes().forEach(cuisine -> chipScroller.add(createCuisineChip(cuisine)));

        Div chips = new Div(left, chipScroller, right);
        chips.addClassName("pm-chip-carousel");
        panel.add(search, chips);
        return panel;
    }

    private Button createCuisineChip(String cuisine) {
        Button chip = new Button(cuisineEmoji(cuisine) + " " + cuisine, event -> {
            if (activeCuisineFilters.contains(cuisine)) {
                activeCuisineFilters.remove(cuisine);
            } else {
                activeCuisineFilters.add(cuisine);
            }
            refreshChipStates();
            refreshRestaurants();
        });
        chip.addClassName("pm-filter-chip");
        chip.getElement().setAttribute("data-cuisine", cuisine);
        return chip;
    }

    private void refreshChipStates() {
        chipScroller.getChildren().forEach(component -> {
            String cuisine = component.getElement().getAttribute("data-cuisine");
            component.getElement().setAttribute("theme", activeCuisineFilters.contains(cuisine) ? "primary" : "");
        });
    }

    private void scrollChips(int direction) {
        chipScroller.getElement().executeJs("this.scrollBy({ left: $0 * 280, behavior: 'smooth' })", direction);
    }

    private void requestBrowserLocation() {
        UI.getCurrent().getPage()
                .executeJs("""
                        return new Promise((resolve) => {
                          if (!navigator.geolocation) {
                            resolve('ERROR');
                            return;
                          }
                          navigator.geolocation.getCurrentPosition(
                            position => resolve(position.coords.latitude + ',' + position.coords.longitude),
                            () => resolve('ERROR'),
                            { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
                          );
                        });
                        """)
                .then(String.class, value -> {
                    if (value == null || value.startsWith("ERROR")) {
                        Notification.show(getTranslation("customer.discover.location.denied"));
                        return;
                    }
                    String[] parts = value.split(",");
                    double latitude = Double.parseDouble(parts[0]);
                    double longitude = Double.parseDouble(parts[1]);
                    locationService.reverseGeocode(latitude, longitude)
                            .ifPresentOrElse(
                                    resolved -> applyLocation(resolved, getTranslation("customer.discover.location.sorted")),
                                    () -> {
                                        customerLatitude = latitude;
                                        customerLongitude = longitude;
                                        location.setValue(getTranslation("customer.discover.location.current"));
                                        locationHint.setText(getTranslation("customer.discover.location.sorted"));
                                        refreshRestaurants();
                                    });
                });
    }

    private void refreshRestaurants() {
        restaurantGrid.removeAll();
        restaurantGrid.addClassName("pm-restaurant-results");

        List<Restaurant> restaurants = restaurantService.findAllRestaurants(getLocale()).stream()
                .filter(this::matchesSearch)
                .filter(this::matchesCuisineFilters)
                .sorted(restaurantComparator())
                .toList();

        if (restaurants.isEmpty()) {
            restaurantGrid.add(emptyState(getTranslation("customer.discover.empty.title"),
                    getTranslation("customer.discover.empty.detail")));
            return;
        }

        restaurants.forEach(restaurant -> restaurantGrid.add(createRestaurantCard(restaurant)));
    }

    private Div createRestaurantCard(Restaurant restaurant) {
        Div card = new Div();
        card.addClassName("pm-customer-restaurant-card");
        if (restaurant.getStatus() == RestaurantStatus.CLOSED) {
            card.addClassName("is-closed");
        }

        Image banner = new Image(imageOrPlaceholder(restaurant.getBannerImageUrl(), PLACEHOLDER_BANNER),
                getTranslation("restaurant.studio.image.bannerAlt", restaurant.getName(getLocale())));
        banner.addClassName("pm-storefront-banner");
        Image logo = new Image(imageOrPlaceholder(restaurant.getLogoImageUrl(), PLACEHOLDER_LOGO),
                getTranslation("restaurant.studio.image.logoAlt", restaurant.getName(getLocale())));
        logo.addClassName("pm-storefront-logo");

        Div details = new Div();
        details.addClassName("pm-storefront-details");
        Div top = new Div();
        top.addClassName("pm-card-title-row");
        top.add(new H2(restaurant.getName(getLocale())), statusPill(restaurant));
        Paragraph description = new Paragraph(restaurant.getDescription(getLocale()));
        Span cuisine = new Span(cuisineEmoji(restaurant.getCategory()) + " " + restaurant.getCategory(getLocale()));
        cuisine.addClassName("pm-cuisine-label");
        Span address = new Span(restaurant.getAddress());
        address.addClassName("pm-muted-line");
        Span hint = new Span(restaurantHint(restaurant));
        hint.addClassName("pm-muted-line");
        details.add(top, cuisine, description, address, hint);

        card.add(banner, logo, details);
        card.addClickListener(event -> openRestaurant(restaurant));
        return card;
    }

    private void openRestaurant(Restaurant restaurant) {
        if (!cartService.isEmpty() && !cartService.belongsTo(restaurant)) {
            showSwitchRestaurantDialog(restaurant);
            return;
        }
        getUI().ifPresent(ui -> ui.navigate(CustomerMenuView.class,
                new RouteParameters("restaurantId", restaurant.getId().toString())));
    }

    private void showSwitchRestaurantDialog(Restaurant target) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("customer.cart.switch.title"));
        dialog.add(new Paragraph(getTranslation("customer.cart.switch.detail", target.getName(getLocale()))));
        Button stay = new Button(getTranslation("customer.cart.switch.stay"), event -> dialog.close());
        Button clear = new Button(getTranslation("customer.cart.switch.clear"), event -> {
            cartService.clear();
            dialog.close();
            openRestaurant(target);
        });
        clear.addClassName("pm-primary-action");
        dialog.getFooter().add(stay, clear);
        dialog.open();
    }

    private Comparator<Restaurant> restaurantComparator() {
        return Comparator
                .comparing((Restaurant restaurant) -> restaurant.getStatus() == RestaurantStatus.CLOSED)
                .thenComparingDouble(this::distanceOrFallback)
                .thenComparing(restaurant -> restaurant.getName(getLocale()));
    }

    private double distanceOrFallback(Restaurant restaurant) {
        if (customerLatitude == null || customerLongitude == null
                || restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
            return 0;
        }
        return distanceKm(customerLatitude, customerLongitude, restaurant.getLatitude(), restaurant.getLongitude());
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean matchesSearch(Restaurant restaurant) {
        String query = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            return true;
        }
        return contains(restaurant.getName(getLocale()), query)
                || contains(restaurant.getCategory(getLocale()), query)
                || contains(restaurant.getDescription(getLocale()), query)
                || contains(restaurant.getAddress(), query);
    }

    private boolean matchesCuisineFilters(Restaurant restaurant) {
        if (activeCuisineFilters.isEmpty()) {
            return true;
        }
        String category = restaurant.getCategory() == null ? "" : restaurant.getCategory();
        return activeCuisineFilters.stream().anyMatch(filter -> filter.equalsIgnoreCase(category));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private List<String> cuisineTypes() {
        return restaurantService.findAllRestaurants(getLocale()).stream()
                .map(Restaurant::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private Span statusPill(Restaurant restaurant) {
        Span status = new Span(getTranslation("restaurantStatus." + restaurant.getStatus().name()));
        status.addClassNames("pm-status-pill", statusClass(restaurant.getStatus()));
        return status;
    }

    private String restaurantHint(Restaurant restaurant) {
        String distance = customerLatitude != null && restaurant.getLatitude() != null
                ? getTranslation("customer.discover.distance", String.format(Locale.US, "%.1f", distanceOrFallback(restaurant)))
                : null;
        if (restaurant.getStatus() == RestaurantStatus.CLOSED) {
            return joinHint(distance, nextOpeningText(restaurant));
        }
        if (restaurant.getStatus() == RestaurantStatus.BUSY) {
            return joinHint(distance, getTranslation("customer.discover.status.busy"));
        }
        return distance == null ? getTranslation("customer.discover.status.open") : distance;
    }

    private String joinHint(String distance, String status) {
        if (distance == null || distance.isBlank()) {
            return status;
        }
        return distance + " · " + status;
    }

    private String nextOpeningText(Restaurant restaurant) {
        Optional<RestaurantOpeningHours> next = nextOpening(restaurant);
        return next.map(hours -> getTranslation("customer.discover.opensAgain",
                        dayLabel(hours.getDayOfWeek()), hours.getOpensAt().format(TIME_FORMAT)))
                .orElseGet(() -> getTranslation("customer.discover.closed"));
    }

    private Optional<RestaurantOpeningHours> nextOpening(Restaurant restaurant) {
        List<RestaurantOpeningHours> hours = new ArrayList<>(restaurantService.findOpeningHours(restaurant));
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        for (int offset = 0; offset < 8; offset++) {
            int dayOffset = offset;
            DayOfWeek day = today.plus(offset);
            LocalTime now = offset == 0 ? LocalTime.now() : LocalTime.MIN;
            Optional<RestaurantOpeningHours> match = hours.stream()
                    .filter(row -> row.getDayOfWeek() == day)
                    .filter(row -> !row.isClosed())
                    .filter(row -> row.getOpensAt() != null)
                    .filter(row -> dayOffset > 0 || row.getOpensAt().isAfter(now))
                    .findFirst();
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private String dayLabel(DayOfWeek day) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        if (day == today) {
            return getTranslation("customer.discover.today");
        }
        if (day == today.plus(1)) {
            return getTranslation("customer.discover.tomorrow");
        }
        return getTranslation("dayOfWeek." + day.name());
    }

    private String statusClass(RestaurantStatus status) {
        return switch (status) {
            case OPEN -> "is-open";
            case BUSY -> "is-busy";
            case CLOSED -> "is-closed";
        };
    }

    private String cuisineEmoji(String cuisine) {
        if (cuisine == null) {
            return "🍽️";
        }
        String normalized = cuisine.toLowerCase(Locale.ROOT);
        if (normalized.contains("pizza")) return "🍕";
        if (normalized.contains("burger")) return "🍔";
        if (normalized.contains("sushi")) return "🍣";
        if (normalized.contains("ramen") || normalized.contains("noodle")) return "🍜";
        if (normalized.contains("cafe") || normalized.contains("coffee")) return "☕";
        if (normalized.contains("mediterranean")) return "🥙";
        if (normalized.contains("vegan")) return "🥗";
        if (normalized.contains("austrian") || normalized.contains("tyrolean")) return "🇦🇹";
        if (normalized.contains("greek")) return "🇬🇷";
        if (normalized.contains("italian") || normalized.contains("pasta")) return "🇮🇹";
        if (normalized.contains("indian") || normalized.contains("curry")) return "🇮🇳";
        if (normalized.contains("nepal")) return "🇳🇵";
        if (normalized.contains("mexican") || normalized.contains("taco")) return "🇲🇽";
        if (normalized.contains("filipino")) return "🇵🇭";
        if (normalized.contains("middle eastern") || normalized.contains("caucasian")) return "🥙";
        return "🍽️";
    }

    private String imageOrPlaceholder(String imageUrl, String placeholder) {
        return imageUrl == null || imageUrl.isBlank() ? placeholder : imageUrl;
    }

    private Div emptyState(String title, String detail) {
        Div empty = new Div();
        empty.addClassName("pm-empty-state");
        empty.add(new H2(title), new Paragraph(detail));
        return empty;
    }
}
