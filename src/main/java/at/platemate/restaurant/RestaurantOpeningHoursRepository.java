package at.platemate.restaurant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantOpeningHoursRepository extends JpaRepository<RestaurantOpeningHours, Long> {

    List<RestaurantOpeningHours> findByRestaurantOrderByDayOfWeekAsc(Restaurant restaurant);

    void deleteByRestaurant(Restaurant restaurant);
}
