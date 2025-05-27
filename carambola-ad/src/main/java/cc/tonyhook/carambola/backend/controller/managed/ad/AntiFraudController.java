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

import cc.tonyhook.carambola.backend.entity.ad.AntiFraud;
import cc.tonyhook.carambola.backend.service.ad.AntiFraudService;
import jakarta.transaction.Transactional;

@RestController
public class AntiFraudController {

    private final AntiFraudService antiFraudService;

    public AntiFraudController(AntiFraudService antiFraudService) {
        this.antiFraudService = antiFraudService;
    }

    @GetMapping(value = "/api/managed/antifraud/port", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<AntiFraud>> getAntiFraudList(
            @RequestParam(required = false) Integer clientPortId,
            Authentication authentication) {
        List<AntiFraud> antiFraudList = antiFraudService.getAntiFraudList(
            authentication,
            clientPortId);

        return ResponseEntity.ok().body(antiFraudList);
    }

    @GetMapping(value = "/api/managed/antifraud/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<AntiFraud> getAntiFraud(
            @PathVariable Integer id,
            Authentication authentication) throws URISyntaxException {
        AntiFraud antiFraud = antiFraudService.getAntiFraud(id);

        if (antiFraud != null) {
            return ResponseEntity.ok().body(antiFraud);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/antifraud", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<AntiFraud> addAntiFraud(
            @RequestBody AntiFraud newAntiFraud,
            Authentication authentication) throws URISyntaxException {
        AntiFraud updatedAntiFraud = antiFraudService.addAntiFraud(newAntiFraud);

        if (updatedAntiFraud != null) {
            return ResponseEntity
                .created(new URI("/api/managed/antifraud/" + updatedAntiFraud.getId()))
                .body(updatedAntiFraud);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/antifraud/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<AntiFraud> updateAntiFraud(
            @PathVariable Integer id,
            @RequestBody AntiFraud newAntiFraud,
            Authentication authentication) throws URISyntaxException {
        if (!id.equals(newAntiFraud.getId())) {
            return ResponseEntity.badRequest().build();
        }

        AntiFraud targetAntiFraud = antiFraudService.getAntiFraud(id);
        if (targetAntiFraud == null) {
            return ResponseEntity.notFound().build();
        }

        antiFraudService.updateAntiFraud(id, newAntiFraud);

        return ResponseEntity.ok().build();

    }

    @Transactional
    @DeleteMapping(value = "/api/managed/antifraud/{id}")
    public ResponseEntity<?> removeAntiFraud(
            @PathVariable Integer id,
            Authentication authentication) {
        AntiFraud targetAntiFraud = antiFraudService.getAntiFraud(id);
        if (targetAntiFraud == null) {
            return ResponseEntity.notFound().build();
        }

        antiFraudService.removeAntiFraud(targetAntiFraud);

        return ResponseEntity.ok().build();
    }

}
