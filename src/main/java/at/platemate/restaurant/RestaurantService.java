package at.platemate.restaurant;

import java.util.List;
import java.util.Locale;

import at.platemate.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantOpeningHoursRepository openingHoursRepository;
    private final RestaurantEventBroadcaster restaurantEventBroadcaster;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            RestaurantOpeningHoursRepository openingHoursRepository,
            RestaurantEventBroadcaster restaurantEventBroadcaster) {
        this.restaurantRepository = restaurantRepository;
        this.openingHoursRepository = openingHoursRepository;
        this.restaurantEventBroadcaster = restaurantEventBroadcaster;
    }

    public List<Restaurant> findOpenRestaurants() {
        return restaurantRepository.findByOpenTrue();
    }

    public List<Restaurant> findAllRestaurants() {
        return restaurantRepository.findAll();
    }

    public List<Restaurant> findAllRestaurants(Locale locale) {
        return findAllRestaurants();
    }

    public List<Restaurant> findOpenRestaurants(Locale locale) {
        return findOpenRestaurants();
    }

    public List<Restaurant> findForOwner(User owner) {
        return restaurantRepository.findByOwner(owner);
    }

    public Restaurant getRestaurant(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + id));
    }

    public Restaurant getRestaurant(Long id, Locale locale) {
        return restaurantRepository.findWithTranslationsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + id));
    }

    @Transactional
    public Restaurant updateProfile(Long restaurantId, String name, String description, String category, String address,
            String logoImageUrl, String bannerImageUrl) {
        Restaurant restaurant = getRestaurant(restaurantId);
        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setCategory(category);
        restaurant.setAddress(address);
        restaurant.setLogoImageUrl(logoImageUrl);
        restaurant.setBannerImageUrl(bannerImageUrl);
        return restaurant;
    }

    @Transactional
    public Restaurant updateStatus(Long restaurantId, RestaurantStatus status) {
        Restaurant restaurant = getRestaurant(restaurantId);
        restaurant.setStatus(status);
        restaurantEventBroadcaster.publish(restaurant);
        return restaurant;
    }

    @Transactional
    public Restaurant updateProfileTranslation(Long restaurantId, Locale locale, String name, String description,
            String category) {
        Restaurant restaurant = getRestaurant(restaurantId);
        RestaurantTranslation translation = restaurant.translation(locale.getLanguage());
        translation.setName(name);
        translation.setDescription(description);
        translation.setCategory(category);
        return restaurant;
    }

    public List<RestaurantOpeningHours> findOpeningHours(Restaurant restaurant) {
        return openingHoursRepository.findByRestaurantOrderByDayOfWeekAsc(restaurant);
    }

    public List<RestaurantOpeningHours> findOpeningHours(Long restaurantId) {
        return findOpeningHours(getRestaurant(restaurantId));
    }

    @Transactional
    public List<RestaurantOpeningHours> replaceOpeningHours(Long restaurantId, List<RestaurantOpeningHours> hours) {
        Restaurant restaurant = getRestaurant(restaurantId);
        openingHoursRepository.deleteByRestaurant(restaurant);
        List<RestaurantOpeningHours> normalizedHours = hours.stream()
                .map(hour -> new RestaurantOpeningHours(
                        restaurant,
                        hour.getDayOfWeek(),
                        hour.getOpensAt(),
                        hour.getClosesAt(),
                        hour.isClosed()))
                .toList();
        return openingHoursRepository.saveAll(normalizedHours);
    }
}
