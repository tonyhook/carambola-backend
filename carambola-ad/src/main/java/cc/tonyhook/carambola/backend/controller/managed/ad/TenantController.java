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

import tools.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.ad.Tenant;
import cc.tonyhook.carambola.backend.entity.ad.TenantDefault;
import cc.tonyhook.carambola.backend.service.ad.TenantDefaultService;
import cc.tonyhook.carambola.backend.service.ad.TenantService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class TenantController {

    private final TenantService tenantService;
    private final TenantDefaultService tenantDefaultService;

    public TenantController(TenantService tenantService, TenantDefaultService tenantDefaultService) {
        this.tenantService = tenantService;
        this.tenantDefaultService = tenantDefaultService;
    }

    @GetMapping(value = "/api/managed/tenant/current", produces = "application/json; charset=UTF-8")
    public ResponseEntity<TenantDefault> getCurrentTenant(
            Authentication authentication) {
        TenantDefault tenantDefault = tenantDefaultService.getTenantDefault(authentication);

        if (tenantDefault == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok().body(tenantDefault);
        }
    }

    @PostMapping(value = "/api/managed/tenant/current", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> switchCurrentTenant(
            @RequestBody Tenant newTenant,
            Authentication authentication) {
        tenantDefaultService.updateTenantDefault(authentication, newTenant);

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/api/managed/tenant", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Tenant>> getTenantList(
            @RequestParam(required = false) String query,
            Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Tenant> tenantList = tenantService.queryTenantList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(tenantList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<Tenant> tenantList = tenantService.getTenantList(authentication);

            return ResponseEntity.ok().body(tenantList);
        }
    }

    @GetMapping(value = "/api/managed/tenant/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Tenant> getTenant(
            @PathVariable Integer id,
            Authentication authentication) {
        Tenant tenant = tenantService.getTenant(authentication, id);

        return ResponseEntity.ok().body(tenant);
    }

    @PostMapping(value = "/api/managed/tenant", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Tenant> addTenant(
            @RequestBody Tenant newTenant,
            Authentication authentication) throws URISyntaxException {
        Tenant updatedTenant = tenantService.addTenant(authentication, newTenant);

        if (updatedTenant != null) {
            return ResponseEntity
                .created(new URI("/api/managed/tenant/" + updatedTenant.getId()))
                .body(updatedTenant);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/tenant/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateTenant(
            @PathVariable Integer id,
            @RequestBody Tenant newTenant,
            Authentication authentication) {
        if (!id.equals(newTenant.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Tenant targetTenant = tenantService.getTenant(authentication, id);
        if (targetTenant == null) {
            return ResponseEntity.notFound().build();
        }

        Tenant updatedTenant = tenantService.updateTenant(authentication, targetTenant, newTenant);

        if (updatedTenant != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/tenant/{id}")
    public ResponseEntity<?> removeTenant(
            @PathVariable Integer id,
            Authentication authentication) {
        Tenant targetTenant = tenantService.getTenant(authentication, id);
        if (targetTenant == null) {
            return ResponseEntity.notFound().build();
        }

        Tenant deletedTenant = tenantService.removeTenant(authentication, targetTenant);

        if (deletedTenant != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
