package at.platemate.order;

import java.util.List;
import java.util.Optional;

import at.platemate.restaurant.Restaurant;
import at.platemate.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"restaurant", "customer", "assignedDriver", "items"})
    Optional<CustomerOrder> findById(Long id);

    @EntityGraph(attributePaths = {"restaurant", "customer", "assignedDriver", "items"})
    List<CustomerOrder> findByCustomerOrderByCreatedAtDesc(User customer);

    @EntityGraph(attributePaths = {"restaurant", "customer", "assignedDriver", "items"})
    List<CustomerOrder> findByGuestSessionIdOrderByCreatedAtDesc(String guestSessionId);

    @EntityGraph(attributePaths = {"restaurant", "customer", "assignedDriver", "items"})
    List<CustomerOrder> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);

    @EntityGraph(attributePaths = {"restaurant", "customer", "assignedDriver", "items"})
    List<CustomerOrder> findByRestaurantInOrderByCreatedAtDesc(List<Restaurant> restaurants);

    @EntityGraph(attributePaths = {"restaurant", "customer", "assignedDriver", "items"})
    List<CustomerOrder> findByAssignedDriverOrderByCreatedAtDesc(User driver);
}
