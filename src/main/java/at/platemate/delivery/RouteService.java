package at.platemate.delivery;

import java.util.Optional;

public interface RouteService {

    RouteEstimate estimateRoute(Optional<GeoPoint> origin, Optional<GeoPoint> destination);
}
