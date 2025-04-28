package cc.tonyhook.carambola.backend.controller.managed.ad;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import cc.tonyhook.carambola.backend.entity.ad.Server;
import cc.tonyhook.carambola.backend.service.ad.AuthenticationService;
import cc.tonyhook.carambola.backend.service.ad.ServerService;
import jakarta.transaction.Transactional;

@RestController
public class ServerController {

    private final AuthenticationService authenticationService;
    private final ServerService serverService;

    public ServerController(AuthenticationService authenticationService, ServerService serverService) {
        this.authenticationService = authenticationService;
        this.serverService = serverService;
    }

    @GetMapping(value = "/api/managed/server/{id}/{service}/{action}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> service(
            @PathVariable Integer id,
            @PathVariable String service,
            @PathVariable String action,
            Authentication authentication) {
        Boolean result = serverService.service(authenticationService.getUsername(authentication), id, service, action);

        if (result) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping(value = "/api/managed/server/status", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Map<Integer, Integer>> getServerStatus(
            Authentication authentication) {
        Map<Integer, Integer> serverStatus = serverService.getServerStatus(authenticationService.getUsername(authentication));

        return ResponseEntity.ok().body(serverStatus);
    }

    @GetMapping(value = "/api/managed/server", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Server>> getServerList(
            Authentication authentication) {
        List<Server> serverList = serverService.getServerList(authenticationService.getUsername(authentication));

        return ResponseEntity.ok().body(serverList);
    }

    @GetMapping(value = "/api/managed/server/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Server> getServer(
            @PathVariable Integer id,
            Authentication authentication) {
        Server server = serverService.getServer(authenticationService.getUsername(authentication), id);

        return ResponseEntity.ok().body(server);
    }

    @PostMapping(value = "/api/managed/server", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Server> addServer(
            @RequestBody Server newServer,
            Authentication authentication) throws URISyntaxException {
        Server updatedServer = serverService.addServer(authenticationService.getUsername(authentication), newServer);

        if (updatedServer != null) {
            return ResponseEntity
                .created(new URI("/api/managed/server/" + updatedServer.getId()))
                .body(updatedServer);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/server/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateServer(
            @PathVariable Integer id,
            @RequestBody Server newServer,
            Authentication authentication) {
        if (!id.equals(newServer.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Server targetServer = serverService.getServer(authenticationService.getUsername(authentication), id);
        if (targetServer == null) {
            return ResponseEntity.notFound().build();
        }

        Server updatedServer = serverService.updateServer(authenticationService.getUsername(authentication), targetServer, newServer);

        if (updatedServer != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/server/{id}")
    public ResponseEntity<?> removeServer(
            @PathVariable Integer id,
            Authentication authentication) {
        Server targetServer = serverService.getServer(authenticationService.getUsername(authentication), id);
        if (targetServer == null) {
            return ResponseEntity.notFound().build();
        }

        Server deletedServer = serverService.removeServer(authenticationService.getUsername(authentication), targetServer);

        if (deletedServer != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
