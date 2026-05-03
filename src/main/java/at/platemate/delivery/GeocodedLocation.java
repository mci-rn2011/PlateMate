package at.platemate.delivery;

public record GeocodedLocation(GeoPoint coordinates, String normalizedAddress) {
}
