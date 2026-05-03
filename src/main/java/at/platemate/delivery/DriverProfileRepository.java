package at.platemate.delivery;

import java.util.List;
import java.util.Optional;

import at.platemate.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<DriverProfile> findByUser(User user);

    @EntityGraph(attributePaths = "user")
    List<DriverProfile> findByStatus(DriverStatus status);
}
