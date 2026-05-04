package at.platemate.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByRole(Role role);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByDisplayName(String displayName);
}
