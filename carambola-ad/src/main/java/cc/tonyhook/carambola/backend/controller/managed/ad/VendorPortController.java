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

import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.ad.VendorMediaService;
import cc.tonyhook.carambola.backend.service.ad.VendorPortService;
import cc.tonyhook.carambola.backend.service.ad.VendorService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class VendorPortController {

    private final VendorService vendorService;
    private final VendorMediaService vendorMediaService;
    private final VendorPortService vendorPortService;

    public VendorPortController(VendorService vendorService, VendorMediaService vendorMediaService, VendorPortService vendorPortService) {
        this.vendorService = vendorService;
        this.vendorMediaService = vendorMediaService;
        this.vendorPortService = vendorPortService;
    }

    @GetMapping(value = "/api/managed/vendorport", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<VendorPort>> getVendorPortList(
            @RequestParam(required = false) String query,
            Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<VendorPort> vendorPortList = vendorPortService.queryVendorPortList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(vendorPortList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<VendorPort> vendorPortList = vendorPortService.getVendorPortList(authentication);

            return ResponseEntity.ok().body(vendorPortList);
        }
    }

    @GetMapping(value = "/api/managed/vendorport/vendor/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<VendorPort>> getVendorPortListByVendor(
            @PathVariable Integer id,
            Authentication authentication) {
        Vendor vendor = vendorService.getVendor(authentication, id);
        List<VendorPort> vendorPortList = vendorPortService.getVendorPortList(authentication, vendor);

        return ResponseEntity.ok().body(vendorPortList);
    }

    @GetMapping(value = "/api/managed/vendorport/vendormedia/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<VendorPort>> getVendorPortListByVendorMedia(
            @PathVariable Integer id,
            Authentication authentication) {
        VendorMedia vendorMedia = vendorMediaService.getVendorMedia(authentication, id);
        List<VendorPort> vendorPortList = vendorPortService.getVendorPortList(authentication, vendorMedia);

        return ResponseEntity.ok().body(vendorPortList);
    }

    @GetMapping(value = "/api/managed/vendorport/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<VendorPort> getVendorPort(
            @PathVariable Integer id,
            Authentication authentication) {
        VendorPort vendorPort = vendorPortService.getVendorPort(authentication, id);

        if (vendorPort != null) {
            return ResponseEntity.ok().body(vendorPort);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/vendorport", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<VendorPort> addVendorPort(
            @RequestBody VendorPort newVendorPort,
            Authentication authentication) throws URISyntaxException {
        VendorPort updatedVendorPort = vendorPortService.addVendorPort(authentication, newVendorPort);

        if (updatedVendorPort != null) {
            return ResponseEntity
                .created(new URI("/api/managed/vendorport/" + updatedVendorPort.getId()))
                .body(updatedVendorPort);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/vendorport/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateVendorPort(
            @PathVariable Integer id,
            @RequestBody VendorPort newVendorPort,
            Authentication authentication) {
        if (!id.equals(newVendorPort.getId())) {
            return ResponseEntity.badRequest().build();
        }

        VendorPort targetVendorPort = vendorPortService.getVendorPort(authentication, id);
        if (targetVendorPort == null) {
            return ResponseEntity.notFound().build();
        }

        VendorPort updatedVendorPort = vendorPortService.updateVendorPort(authentication, targetVendorPort, newVendorPort);

        if (updatedVendorPort != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/vendorport/{id}")
    public ResponseEntity<?> removeVendorPort(
            @PathVariable Integer id,
            Authentication authentication) {
        VendorPort targetVendorPort = vendorPortService.getVendorPort(authentication, id);

        if (targetVendorPort == null) {
            return ResponseEntity.notFound().build();
        }

        VendorPort deletedVendorPort = vendorPortService.removeVendorPort(authentication, targetVendorPort);

        if (deletedVendorPort != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
