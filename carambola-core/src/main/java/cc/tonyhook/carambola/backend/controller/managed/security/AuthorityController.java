package cc.tonyhook.carambola.backend.controller.managed.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.security.Authority;
import cc.tonyhook.carambola.backend.service.security.AuthorityService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class AuthorityController {

    private final AuthorityService authorityService;

    public AuthorityController(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @GetMapping(value = "/api/managed/authority", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Authority>> getAuthorityList(
            @RequestParam(required = false) String query) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Authority> authorityList = authorityService.queryAuthorityList(objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(authorityList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<Authority> authorityList = authorityService.getAuthorityList();

            return ResponseEntity.ok().body(authorityList);
        }
    }

    @GetMapping(value = "/api/managed/authority/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Authority> getAuthority(
            @PathVariable Integer id) {
        Authority authority = authorityService.getAuthority(id);

        if (authority != null) {
            return ResponseEntity.ok().body(authority);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/authority")
    public ResponseEntity<Authority> addAuthority(
            @RequestBody Authority newAuthority) throws URISyntaxException {
        Authority updatedAuthority = authorityService.addAuthority(newAuthority);

        return ResponseEntity
                .created(new URI("/api/managed/authority/" + updatedAuthority.getId()))
                .body(updatedAuthority);
    }

    @PutMapping(value = "/api/managed/authority/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateAuthority(
            @PathVariable Integer id,
            @RequestBody Authority newAuthority) {
        if (!id.equals(newAuthority.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Authority targetAuthority = authorityService.getAuthority(id);
        if (targetAuthority == null) {
            return ResponseEntity.notFound().build();
        }

        authorityService.updateAuthority(id, newAuthority);

        return ResponseEntity.ok().build();
    }

    @Transactional
    @DeleteMapping("/api/managed/authority/{id}")
    public ResponseEntity<?> removeAuthority(
            @PathVariable Integer id) {
        Authority deletedAuthority = authorityService.getAuthority(id);
        if (deletedAuthority == null) {
            return ResponseEntity.notFound().build();
        }

        authorityService.removeAuthority(id);

        return ResponseEntity.ok().build();
    }

}
