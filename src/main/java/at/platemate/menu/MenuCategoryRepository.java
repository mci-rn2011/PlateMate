package at.platemate.menu;

import java.util.List;

import at.platemate.restaurant.Restaurant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    @EntityGraph(attributePaths = "translations")
    List<MenuCategory> findByRestaurantOrderBySortOrderAscNameAsc(Restaurant restaurant);
}
