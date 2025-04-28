package cc.tonyhook.carambola.backend.controller.open;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cc.tonyhook.carambola.backend.entity.ad.Server;
import cc.tonyhook.carambola.backend.service.ad.ServerService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class OpenAdController {

    private final ServerService serverService;

    public OpenAdController(ServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping(value = "/api/open/server", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Server> getServer(HttpServletRequest request) {
        Server server = serverService.getServer(request.getHeader("X-Forwarded-For"));
        if (server == null) {
            server = serverService.getServer(request.getRemoteAddr());
        }

        if (server != null) {
            return ResponseEntity.ok().body(server);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
