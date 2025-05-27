package cc.tonyhook.carambola.backend.controller.managed.ad;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import cc.tonyhook.carambola.backend.entity.ad.AntiFraudRule;
import cc.tonyhook.carambola.backend.service.ad.AuthenticationService;
import cc.tonyhook.carambola.backend.service.ad.AntiFraudRuleService;
import jakarta.transaction.Transactional;

@RestController
public class AntiFraudRuleController {

    private final AuthenticationService authenticationService;
    private final AntiFraudRuleService antiFraudRuleService;

    public AntiFraudRuleController(AuthenticationService authenticationService, AntiFraudRuleService antiFraudRuleService) {
        this.authenticationService = authenticationService;
        this.antiFraudRuleService = antiFraudRuleService;
    }

    @GetMapping(value = "/api/managed/antifraudrule", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<AntiFraudRule>> getAntiFraudRuleList(
            Authentication authentication) {
        List<AntiFraudRule> antiFraudRuleList = antiFraudRuleService.getAntiFraudRuleList(authenticationService.getUsername(authentication));

        return ResponseEntity.ok().body(antiFraudRuleList);
    }

    @GetMapping(value = "/api/managed/antifraudrule/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<AntiFraudRule> getAntiFraudRule(
            @PathVariable Integer id,
            Authentication authentication) {
        AntiFraudRule antiFraudRule = antiFraudRuleService.getAntiFraudRule(authenticationService.getUsername(authentication), id);

        return ResponseEntity.ok().body(antiFraudRule);
    }

    @GetMapping(value = "/api/managed/antifraudrule/code/{code}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<AntiFraudRule> getAntiFraudRule(
            @PathVariable String code,
            Authentication authentication) {
        AntiFraudRule antiFraudRule = antiFraudRuleService.getAntiFraudRule(authenticationService.getUsername(authentication), code);

        return ResponseEntity.ok().body(antiFraudRule);
    }

    @PostMapping(value = "/api/managed/antifraudrule", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<AntiFraudRule> addAntiFraudRule(
            @RequestBody AntiFraudRule newAntiFraudRule,
            Authentication authentication) throws URISyntaxException {
        AntiFraudRule updatedAntiFraudRule = antiFraudRuleService.addAntiFraudRule(authenticationService.getUsername(authentication), newAntiFraudRule);

        if (updatedAntiFraudRule != null) {
            return ResponseEntity
                .created(new URI("/api/managed/antifraudrule/" + updatedAntiFraudRule.getId()))
                .body(updatedAntiFraudRule);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/antifraudrule/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateAntiFraudRule(
            @PathVariable Integer id,
            @RequestBody AntiFraudRule newAntiFraudRule,
            Authentication authentication) {
        if (!id.equals(newAntiFraudRule.getId())) {
            return ResponseEntity.badRequest().build();
        }

        AntiFraudRule targetAntiFraudRule = antiFraudRuleService.getAntiFraudRule(authenticationService.getUsername(authentication), id);
        if (targetAntiFraudRule == null) {
            return ResponseEntity.notFound().build();
        }

        AntiFraudRule updatedAntiFraudRule = antiFraudRuleService.updateAntiFraudRule(authenticationService.getUsername(authentication), targetAntiFraudRule, newAntiFraudRule);

        if (updatedAntiFraudRule != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/antifraudrule/{id}")
    public ResponseEntity<?> removeAntiFraudRule(
            @PathVariable Integer id,
            Authentication authentication) {
        AntiFraudRule targetAntiFraudRule = antiFraudRuleService.getAntiFraudRule(authenticationService.getUsername(authentication), id);
        if (targetAntiFraudRule == null) {
            return ResponseEntity.notFound().build();
        }

        AntiFraudRule deletedAntiFraudRule = antiFraudRuleService.removeAntiFraudRule(authenticationService.getUsername(authentication), targetAntiFraudRule);

        if (deletedAntiFraudRule != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
