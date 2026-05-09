package at.platemate.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MapboxReadyRouteServiceTest {

    @Test
    void blankTokenDisablesMapboxAndStaticPreview() {
        MapboxReadyRouteService service = new MapboxReadyRouteService("   ");

        assertThat(service.hasMapboxToken()).isFalse();
        assertThat(service.buildStaticPreviewUrl(
                new GeoPoint(48.2082, 16.3738),
                new GeoPoint(48.2167, 16.4000))).isNull();
    }

    @Test
    void missingOriginOrDestinationUsesFixedFallbackEstimate() {
        MapboxReadyRouteService service = new MapboxReadyRouteService("");

        RouteEstimate missingOrigin = service.estimateRoute(
                Optional.empty(),
                Optional.of(new GeoPoint(48.2167, 16.4000)));
        RouteEstimate missingDestination = service.estimateRoute(
                Optional.of(new GeoPoint(48.2082, 16.3738)),
                Optional.empty());

        assertThat(missingOrigin).isEqualTo(new RouteEstimate(new BigDecimal("4.20"), 18, null));
        assertThat(missingDestination).isEqualTo(new RouteEstimate(new BigDecimal("4.20"), 18, null));
    }

    @Test
    void noTokenCalculatesOfflineEstimateFromCoordinates() {
        MapboxReadyRouteService service = new MapboxReadyRouteService("");

        RouteEstimate estimate = service.estimateRoute(
                Optional.of(new GeoPoint(48.2082, 16.3738)),
                Optional.of(new GeoPoint(48.2167, 16.4000)));

        assertThat(estimate.distanceKm()).isEqualByComparingTo("2.70");
        assertThat(estimate.estimatedMinutes()).isEqualTo(9);
        assertThat(estimate.previewUrl()).isNull();
    }

    @Test
    void noTokenEstimateHasMinimumDistanceAndDurationForSamePoint() {
        MapboxReadyRouteService service = new MapboxReadyRouteService(null);
        GeoPoint point = new GeoPoint(48.2082, 16.3738);

        RouteEstimate estimate = service.estimateRoute(Optional.of(point), Optional.of(point));

        assertThat(estimate.distanceKm()).isEqualByComparingTo("0.80");
        assertThat(estimate.estimatedMinutes()).isEqualTo(5);
        assertThat(estimate.previewUrl()).isNull();
    }
}
