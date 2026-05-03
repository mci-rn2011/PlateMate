package at.platemate.delivery;

import java.math.BigDecimal;

public record RouteEstimate(BigDecimal distanceKm, int estimatedMinutes, String previewUrl) {
}
