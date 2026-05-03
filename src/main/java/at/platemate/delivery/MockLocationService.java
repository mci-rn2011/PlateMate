package at.platemate.delivery;

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
public class MockLocationService implements LocationService {

    private final String mapboxToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MockLocationService(@Value("${platemate.maps.mapbox-token:}") String mapboxToken) {
        this.mapboxToken = mapboxToken == null ? "" : mapboxToken.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    @Override
    public Optional<GeocodedLocation> forwardGeocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        if (!mapboxToken.isBlank()) {
            Optional<GeocodedLocation> realLocation = fetchForwardGeocode(address.trim());
            if (realLocation.isPresent()) {
                return realLocation;
            }
        }
        return Optional.of(new GeocodedLocation(mockPoint(address), address.trim()));
    }

    @Override
    public Optional<GeocodedLocation> reverseGeocode(double latitude, double longitude) {
        if (!mapboxToken.isBlank()) {
            Optional<GeocodedLocation> realLocation = fetchReverseGeocode(latitude, longitude);
            if (realLocation.isPresent()) {
                return realLocation;
            }
        }
        return Optional.of(new GeocodedLocation(
                new GeoPoint(latitude, longitude),
                String.format("%.5f, %.5f", latitude, longitude)));
    }

    private Optional<GeocodedLocation> fetchForwardGeocode(String address) {
        try {
            URI uri = URI.create("https://api.mapbox.com/geocoding/v5/mapbox.places/"
                    + url(address) + ".json?limit=1&country=AT&access_token=" + url(mapboxToken));
            return parseGeocode(send(uri));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<GeocodedLocation> fetchReverseGeocode(double latitude, double longitude) {
        try {
            URI uri = URI.create("https://api.mapbox.com/geocoding/v5/mapbox.places/"
                    + longitude + "," + latitude + ".json?limit=1&access_token=" + url(mapboxToken));
            return parseGeocode(send(uri));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String send(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return "";
        }
        return response.body();
    }

    private Optional<GeocodedLocation> parseGeocode(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        JsonNode feature = objectMapper.readTree(body).path("features").path(0);
        if (feature.isMissingNode()) {
            return Optional.empty();
        }
        JsonNode center = feature.path("center");
        if (!center.isArray() || center.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(new GeocodedLocation(
                new GeoPoint(center.get(1).asDouble(), center.get(0).asDouble()),
                feature.path("place_name").asText("")));
    }

    private GeoPoint mockPoint(String seed) {
        int hash = Math.abs(seed.toLowerCase().hashCode());
        double latitude = 48.2082 + ((hash % 1000) - 500) / 100000.0;
        double longitude = 16.3738 + (((hash / 1000) % 1000) - 500) / 100000.0;
        return new GeoPoint(latitude, longitude);
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
