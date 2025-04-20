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
import cc.tonyhook.carambola.backend.service.ad.VendorMediaService;
import cc.tonyhook.carambola.backend.service.ad.VendorService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class VendorMediaController {

    private final VendorService vendorService;
    private final VendorMediaService vendorMediaService;

    public VendorMediaController(VendorService vendorService, VendorMediaService vendorMediaService) {
        this.vendorService = vendorService;
        this.vendorMediaService = vendorMediaService;
    }

    @GetMapping(value = "/api/managed/vendormedia", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<VendorMedia>> getVendorMediaList(
            @RequestParam(required = false) String query,
            Authentication authentication) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<VendorMedia> vendorMediaList = vendorMediaService.queryVendorMediaList(authentication, objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(vendorMediaList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<VendorMedia> vendorMediaList = vendorMediaService.getVendorMediaList(authentication);

            return ResponseEntity.ok().body(vendorMediaList);
        }
    }

    @GetMapping(value = "/api/managed/vendormedia/vendor/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<VendorMedia>> getVendorMediaListByVendor(
            @PathVariable Integer id,
            Authentication authentication) {
        Vendor vendor = vendorService.getVendor(authentication, id);
        List<VendorMedia> vendorMediaList = vendorMediaService.getVendorMediaList(authentication, vendor);

        return ResponseEntity.ok().body(vendorMediaList);
    }

    @GetMapping(value = "/api/managed/vendormedia/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<VendorMedia> getVendorMedia(
            @PathVariable Integer id,
            Authentication authentication) {
        VendorMedia vendorMedia = vendorMediaService.getVendorMedia(authentication, id);

        if (vendorMedia != null) {
            return ResponseEntity.ok().body(vendorMedia);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/vendormedia", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<VendorMedia> addVendorMedia(
            @RequestBody VendorMedia newVendorMedia,
            Authentication authentication) throws URISyntaxException {
        VendorMedia updatedVendorMedia = vendorMediaService.addVendorMedia(authentication, newVendorMedia);

        if (updatedVendorMedia != null) {
            return ResponseEntity
                .created(new URI("/api/managed/vendormedia/" + updatedVendorMedia.getId()))
                .body(updatedVendorMedia);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping(value = "/api/managed/vendormedia/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateVendorMedia(
            @PathVariable Integer id,
            @RequestBody VendorMedia newVendorMedia,
            Authentication authentication) {
        if (!id.equals(newVendorMedia.getId())) {
            return ResponseEntity.badRequest().build();
        }

        VendorMedia targetVendorMedia = vendorMediaService.getVendorMedia(authentication, id);
        if (targetVendorMedia == null) {
            return ResponseEntity.notFound().build();
        }

        VendorMedia updatedVendorMedia = vendorMediaService.updateVendorMedia(authentication, targetVendorMedia, newVendorMedia);

        if (updatedVendorMedia != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Transactional
    @DeleteMapping("/api/managed/vendormedia/{id}")
    public ResponseEntity<?> removeVendorMedia(
            @PathVariable Integer id,
            Authentication authentication) {
        VendorMedia targetVendorMedia = vendorMediaService.getVendorMedia(authentication, id);

        if (targetVendorMedia == null) {
            return ResponseEntity.notFound().build();
        }

        VendorMedia deletedVendorMedia = vendorMediaService.removeVendorMedia(authentication, targetVendorMedia);

        if (deletedVendorMedia != null) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
