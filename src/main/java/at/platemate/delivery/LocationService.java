package at.platemate.delivery;

import java.util.Optional;

public interface LocationService {

    Optional<GeocodedLocation> forwardGeocode(String address);

    Optional<GeocodedLocation> reverseGeocode(double latitude, double longitude);
}
