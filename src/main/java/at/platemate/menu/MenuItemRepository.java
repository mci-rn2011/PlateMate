package at.platemate.menu;

import java.util.List;
import java.util.Optional;

import at.platemate.restaurant.Restaurant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @EntityGraph(attributePaths = {"translations", "category", "category.translations"})
    List<MenuItem> findByRestaurantAndAvailableTrue(Restaurant restaurant);

    @EntityGraph(attributePaths = {"translations", "category", "category.translations"})
    List<MenuItem> findByRestaurant(Restaurant restaurant);

    @EntityGraph(attributePaths = {"translations", "category", "category.translations"})
    List<MenuItem> findByRestaurantOrderBySortOrderAscNameAsc(Restaurant restaurant);

    @EntityGraph(attributePaths = {"translations", "category", "category.translations"})
    List<MenuItem> findByRestaurantAndAvailableTrueOrderBySortOrderAscNameAsc(Restaurant restaurant);

    long countByCategory(MenuCategory category);

    @EntityGraph(attributePaths = {"translations", "category", "category.translations"})
    Optional<MenuItem> findByRestaurantAndName(Restaurant restaurant, String name);
}
