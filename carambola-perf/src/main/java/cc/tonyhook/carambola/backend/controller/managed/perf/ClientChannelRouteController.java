package cc.tonyhook.carambola.backend.controller.managed.perf;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;
import cc.tonyhook.carambola.backend.service.perf.ClientChannelRouteService;

@RequestMapping("/api/managed/perf")
@RestController
public class ClientChannelRouteController {

    public record RouteView(
        Integer id,
        Integer clientChannelId,
        String event,
        String trackName,
        String trackCode
    ) {}

    private final ClientChannelRouteService clientChannelRouteService;

    public ClientChannelRouteController(ClientChannelRouteService clientChannelRouteService) {
        this.clientChannelRouteService = clientChannelRouteService;
    }

    @GetMapping(value = "/client-channel-route", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<RouteView>> getRoutes(Authentication authentication) {
        List<RouteView> routes = clientChannelRouteService.getRoutes(authentication).stream()
            .map(route -> new RouteView(
                route.getId(),
                route.getClientChannel().getId(),
                route.getEvent(),
                route.getTrackName(),
                route.getTrackCode()))
            .toList();
        return ResponseEntity.ok(routes);
    }

    @GetMapping(value = "/client-channel/{clientChannelId}/route", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<ClientChannelRoute>> getRoutes(
            @PathVariable Integer clientChannelId,
            Authentication authentication
    ) {
        List<ClientChannelRoute> routes = clientChannelRouteService.getRoutes(authentication, clientChannelId);
        return routes == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(routes);
    }

    @PostMapping(value = "/client-channel/{clientChannelId}/route", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<ClientChannelRoute> addRoute(
            @PathVariable Integer clientChannelId,
            @RequestBody ClientChannelRoute newRoute,
            Authentication authentication
    ) throws URISyntaxException {
        ClientChannelRoute route = clientChannelRouteService.addRoute(authentication, clientChannelId, newRoute);
        if (route == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity
            .created(new URI("/api/managed/perf/client-channel/" + clientChannelId + "/route/" + route.getId()))
            .body(route);
    }

    @PutMapping(value = "/client-channel/{clientChannelId}/route/{routeId}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateRoute(
            @PathVariable Integer clientChannelId,
            @PathVariable Integer routeId,
            @RequestBody ClientChannelRoute newRoute,
            Authentication authentication
    ) {
        if (newRoute.getId() != null && !routeId.equals(newRoute.getId())) {
            return ResponseEntity.badRequest().build();
        }
        ClientChannelRoute route = clientChannelRouteService.updateRoute(
            authentication, clientChannelId, routeId, newRoute);
        return route == null
            ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            : ResponseEntity.ok().build();
    }

    @DeleteMapping("/client-channel/{clientChannelId}/route/{routeId}")
    public ResponseEntity<?> removeRoute(
            @PathVariable Integer clientChannelId,
            @PathVariable Integer routeId,
            Authentication authentication
    ) {
        ClientChannelRoute route = clientChannelRouteService.removeRoute(authentication, clientChannelId, routeId);
        return route == null
            ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            : ResponseEntity.ok().build();
    }

}
