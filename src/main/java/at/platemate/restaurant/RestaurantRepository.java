package at.platemate.restaurant;

import java.util.List;
import java.util.Optional;

import at.platemate.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @EntityGraph(attributePaths = "translations")
    List<Restaurant> findByOpenTrue();

    @Override
    @EntityGraph(attributePaths = "translations")
    List<Restaurant> findAll();

    @EntityGraph(attributePaths = "translations")
    List<Restaurant> findByOwner(User owner);

    @EntityGraph(attributePaths = "translations")
    Optional<Restaurant> findWithTranslationsById(Long id);

    Optional<Restaurant> findByName(String name);
}
