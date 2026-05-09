package at.platemate.delivery;

import java.util.List;
import java.util.Optional;

public interface LocationService {

    Optional<GeocodedLocation> forwardGeocode(String address);

    default List<GeocodedLocation> searchForwardGeocode(String address, int limit) {
        return forwardGeocode(address).stream().toList();
    }

    Optional<GeocodedLocation> reverseGeocode(double latitude, double longitude);
}
