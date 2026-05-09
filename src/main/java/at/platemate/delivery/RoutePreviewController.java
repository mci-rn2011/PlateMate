package at.platemate.delivery;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RoutePreviewController {

    private final MapboxReadyRouteService routeService;

    public RoutePreviewController(MapboxReadyRouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/api/route-preview")
    public ResponseEntity<Void> preview(
            @RequestParam double originLat,
            @RequestParam double originLon,
            @RequestParam double destinationLat,
            @RequestParam double destinationLon) {
        if (!routeService.hasMapboxToken()) {
            return redirect("/placeholders/route-preview.svg");
        }
        String previewUrl = routeService.buildStaticPreviewUrl(
                new GeoPoint(originLat, originLon),
                new GeoPoint(destinationLat, destinationLon));
        if (previewUrl == null || previewUrl.isBlank()) {
            return redirect("/placeholders/route-preview.svg");
        }
        return redirect(previewUrl);
    }

    private ResponseEntity<Void> redirect(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
