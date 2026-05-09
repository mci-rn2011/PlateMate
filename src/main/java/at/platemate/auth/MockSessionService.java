package at.platemate.auth;

import java.util.Optional;
import java.util.UUID;

import at.platemate.delivery.GeocodedLocation;
import at.platemate.user.Role;
import at.platemate.user.User;
import at.platemate.user.UserRepository;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Service;

@Service
@VaadinSessionScope
public class MockSessionService {

    private final UserRepository userRepository;
    private User currentUser;
    private final String guestSessionId = UUID.randomUUID().toString();
    private GeocodedLocation selectedDeliveryLocation;

    public MockSessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public String getGuestSessionId() {
        return guestSessionId;
    }

    public boolean isGuest() {
        return currentUser != null && "guest".equalsIgnoreCase(currentUser.getUsername());
    }

    public boolean login(String username, String password) {
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .filter(user -> user.passwordMatches(password))
                .map(user -> {
                    login(user);
                    return true;
                })
                .orElse(false);
    }

    public User loginAsGuest() {
        User guest = userRepository.findByUsernameIgnoreCase("guest")
                .orElseGet(() -> userRepository.save(new User("Guest", "guest", "guest", Role.CUSTOMER)));
        login(guest);
        return guest;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public Optional<GeocodedLocation> getSelectedDeliveryLocation() {
        return Optional.ofNullable(selectedDeliveryLocation);
    }

    public void setSelectedDeliveryLocation(GeocodedLocation selectedDeliveryLocation) {
        this.selectedDeliveryLocation = selectedDeliveryLocation;
    }

    public void clearSelectedDeliveryLocation() {
        this.selectedDeliveryLocation = null;
    }

    public void logout() {
        this.currentUser = null;
    }
}
