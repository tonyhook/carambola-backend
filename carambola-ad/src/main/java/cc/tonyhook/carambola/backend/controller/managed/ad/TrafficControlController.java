package cc.tonyhook.carambola.backend.controller.managed.ad;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.ad.TrafficControl;
import cc.tonyhook.carambola.backend.service.ad.TrafficControlService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class TrafficControlController {

    private final TrafficControlService trafficControlService;

    public TrafficControlController(TrafficControlService trafficControlService) {
        this.trafficControlService = trafficControlService;
    }

    @GetMapping(value = "/api/managed/trafficcontrol/query", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<TrafficControl>> getTrafficControlList(
            @RequestParam(required = false) String query,
            Authentication authentication) {
        Query queryObject = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }

        List<TrafficControl> trafficControlList = trafficControlService.getTrafficControlList(
            authentication,
            queryObject);

        return ResponseEntity.ok().body(trafficControlList);
    }

    @GetMapping(value = "/api/managed/trafficcontrol/port", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<TrafficControl>> getTrafficControlList(
            @RequestParam(required = false) Integer clientPortId,
            @RequestParam(required = false) Integer vendorPortId,
            Authentication authentication) {
        List<TrafficControl> trafficControlList = trafficControlService.getTrafficControlList(
            authentication,
            clientPortId,
            vendorPortId);

        return ResponseEntity.ok().body(trafficControlList);
    }

    @GetMapping(value = "/api/managed/trafficcontrol/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<TrafficControl> getTrafficControl(
            @PathVariable Integer id,
            Authentication authentication) throws URISyntaxException {
        TrafficControl trafficControl = trafficControlService.getTrafficControl(id);

        if (trafficControl != null) {
            return ResponseEntity.ok().body(trafficControl);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/trafficcontrol", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<TrafficControl> addTrafficControl(
            @RequestBody TrafficControl newTrafficControl,
            Authentication authentication) throws URISyntaxException {
        TrafficControl updatedTrafficControl = trafficControlService.addTrafficControl(newTrafficControl);

        if (updatedTrafficControl != null) {
            return ResponseEntity
                .created(new URI("/api/managed/trafficcontrol/" + updatedTrafficControl.getId()))
                .body(updatedTrafficControl);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/trafficcontrol/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<TrafficControl> updateTrafficControl(
            @PathVariable Integer id,
            @RequestBody TrafficControl newTrafficControl,
            Authentication authentication) throws URISyntaxException {
        if (!id.equals(newTrafficControl.getId())) {
            return ResponseEntity.badRequest().build();
        }

        TrafficControl targetTrafficControl = trafficControlService.getTrafficControl(id);
        if (targetTrafficControl == null) {
            return ResponseEntity.notFound().build();
        }

        trafficControlService.updateTrafficControl(id, newTrafficControl);

        return ResponseEntity.ok().build();

    }

    @Transactional
    @DeleteMapping(value = "/api/managed/trafficcontrol/{id}")
    public ResponseEntity<?> removeTrafficControl(
            @PathVariable Integer id,
            Authentication authentication) {
        TrafficControl targetTrafficControl = trafficControlService.getTrafficControl(id);
        if (targetTrafficControl == null) {
            return ResponseEntity.notFound().build();
        }

        trafficControlService.removeTrafficControl(targetTrafficControl);

        return ResponseEntity.ok().build();
    }

}
