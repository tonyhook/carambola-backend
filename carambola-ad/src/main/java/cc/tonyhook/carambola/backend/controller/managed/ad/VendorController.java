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

import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.service.ad.VendorService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping(value = "/api/managed/vendor", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Vendor>> getVendorList(
            @RequestParam(required = false) String query,
            Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Vendor> vendorList = vendorService.queryVendorList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(vendorList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<Vendor> vendorList = vendorService.getVendorList(authentication);

            return ResponseEntity.ok().body(vendorList);
        }
    }

    @GetMapping(value = "/api/managed/vendor/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Vendor> getVendor(
            @PathVariable Integer id,
            Authentication authentication) {
        Vendor vendor = vendorService.getVendor(authentication, id);

        if (vendor != null) {
            return ResponseEntity.ok().body(vendor);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/vendor", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Vendor> addVendor(
            @RequestBody Vendor newVendor,
            Authentication authentication) throws URISyntaxException {
        Vendor updatedVendor = vendorService.addVendor(authentication, newVendor);

        if (updatedVendor != null) {
            return ResponseEntity
                .created(new URI("/api/managed/vendor/" + updatedVendor.getId()))
                .body(updatedVendor);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/vendor/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateVendor(
            @PathVariable Integer id,
            @RequestBody Vendor newVendor,
            Authentication authentication) {
        if (!id.equals(newVendor.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Vendor targetVendor = vendorService.getVendor(authentication, id);
        if (targetVendor == null) {
            return ResponseEntity.notFound().build();
        }

        Vendor updatedVendor = vendorService.updateVendor(authentication, targetVendor, newVendor);

        if (updatedVendor != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/vendor/{id}")
    public ResponseEntity<?> removeVendor(
            @PathVariable Integer id,
            Authentication authentication) {
        Vendor targetVendor = vendorService.getVendor(authentication, id);

        if (targetVendor == null) {
            return ResponseEntity.notFound().build();
        }

        Vendor deletedVendor = vendorService.removeVendor(authentication, targetVendor);

        if (deletedVendor != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
