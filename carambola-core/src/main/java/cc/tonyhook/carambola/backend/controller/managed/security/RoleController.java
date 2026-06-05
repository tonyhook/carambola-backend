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

import cc.tonyhook.carambola.backend.entity.security.Role;
import cc.tonyhook.carambola.backend.service.security.RoleService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@RestController
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping(value = "/api/managed/role", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Role>> getRoleList(
            @RequestParam(required = false) String query) {
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Role> roleList = roleService.queryRoleList(objectMapper.readValue(query, Query.class));

                return ResponseEntity.ok().body(roleList);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            List<Role> roleList = roleService.getRoleList();

            return ResponseEntity.ok().body(roleList);
        }
    }

    @GetMapping(value = "/api/managed/role/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Role> getRole(
            @PathVariable Integer id) {
        Role role = roleService.getRole(id);

        if (role != null) {
            return ResponseEntity.ok().body(role);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/role", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Role> addRole(
            @RequestBody Role newRole) throws URISyntaxException {
        Role updatedRole = roleService.addRole(newRole);

        return ResponseEntity
                .created(new URI("/api/managed/role/" + updatedRole.getId()))
                .body(updatedRole);
    }

    @PutMapping(value = "/api/managed/role/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updateRole(
            @PathVariable Integer id,
            @RequestBody Role newRole) {
        if (!id.equals(newRole.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Role targetRole = roleService.getRole(id);
        if (targetRole == null) {
            return ResponseEntity.notFound().build();
        }

        roleService.updateRole(id, newRole);

        return ResponseEntity.ok().build();
    }

    @Transactional
    @DeleteMapping("/api/managed/role/{id}")
    public ResponseEntity<?> removeRole(
            @PathVariable Integer id) {
        Role deletedRole = roleService.getRole(id);
        if (deletedRole == null) {
            return ResponseEntity.notFound().build();
        }

        roleService.removeRole(id);

        return ResponseEntity.ok().build();
    }

}
