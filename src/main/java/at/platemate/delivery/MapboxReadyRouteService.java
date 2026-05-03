package at.platemate.delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MapboxReadyRouteService implements RouteService {

    private static final BigDecimal FALLBACK_DISTANCE_KM = new BigDecimal("4.20");
    private static final int FALLBACK_MINUTES = 18;
    private static final String STATIC_STYLE = "mapbox/streets-v12";

    private final String mapboxToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MapboxReadyRouteService(@Value("${platemate.maps.mapbox-token:}") String mapboxToken) {
        this.mapboxToken = mapboxToken == null ? "" : mapboxToken.trim();
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    @Override
    public RouteEstimate estimateRoute(Optional<GeoPoint> origin, Optional<GeoPoint> destination) {
        if (origin.isEmpty() || destination.isEmpty()) {
            return new RouteEstimate(FALLBACK_DISTANCE_KM, FALLBACK_MINUTES, null);
        }

        if (!mapboxToken.isBlank()) {
            Optional<RouteEstimate> mapboxRoute = fetchMapboxRoute(origin.get(), destination.get());
            if (mapboxRoute.isPresent()) {
                return mapboxRoute.get();
            }
        }

        BigDecimal distance = haversineKm(origin.get(), destination.get())
                .multiply(new BigDecimal("1.25"))
                .max(new BigDecimal("0.80"))
                .setScale(2, RoundingMode.HALF_UP);
        int minutes = Math.max(5, distance.multiply(new BigDecimal("3.0")).setScale(0, RoundingMode.CEILING).intValue());
        return new RouteEstimate(distance, minutes, staticPreviewUrl(origin.get(), destination.get(), null));
    }

    private Optional<RouteEstimate> fetchMapboxRoute(GeoPoint origin, GeoPoint destination) {
        try {
            URI uri = URI.create("https://api.mapbox.com/directions/v5/mapbox/driving/"
                    + coordinate(origin) + ";" + coordinate(destination)
                    + "?overview=full&geometries=polyline&access_token=" + url(mapboxToken));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }
            JsonNode firstRoute = objectMapper.readTree(response.body()).path("routes").path(0);
            if (firstRoute.isMissingNode()) {
                return Optional.empty();
            }
            BigDecimal distanceKm = BigDecimal.valueOf(firstRoute.path("distance").asDouble() / 1000.0)
                    .setScale(2, RoundingMode.HALF_UP);
            int minutes = Math.max(1, (int) Math.ceil(firstRoute.path("duration").asDouble() / 60.0));
            String polyline = firstRoute.path("geometry").asText(null);
            return Optional.of(new RouteEstimate(distanceKm, minutes, staticPreviewUrl(origin, destination, polyline)));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private BigDecimal haversineKm(GeoPoint origin, GeoPoint destination) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(destination.latitude() - origin.latitude());
        double dLon = Math.toRadians(destination.longitude() - origin.longitude());
        double lat1 = Math.toRadians(origin.latitude());
        double lat2 = Math.toRadians(destination.latitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadiusKm * c);
    }

    private String staticPreviewUrl(GeoPoint origin, GeoPoint destination, String encodedPolyline) {
        if (mapboxToken.isBlank()) {
            return null;
        }
        String routeOverlay = encodedPolyline == null || encodedPolyline.isBlank()
                ? ""
                : ",path-5+2f7d32-0.92(" + url(encodedPolyline) + ")";
        String overlay = "pin-s-a+2f7d32(" + coordinate(origin) + "),pin-s-b+111111(" + coordinate(destination) + ")"
                + routeOverlay;
        return "https://api.mapbox.com/styles/v1/" + STATIC_STYLE + "/static/"
                + overlay + "/auto/760x320@2x?padding=48&access_token=" + url(mapboxToken);
    }

    private String coordinate(GeoPoint point) {
        return point.longitude() + "," + point.latitude();
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
