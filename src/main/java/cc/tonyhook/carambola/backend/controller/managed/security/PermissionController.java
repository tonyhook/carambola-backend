package cc.tonyhook.carambola.backend.controller.managed.security;

import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cc.tonyhook.carambola.backend.entity.ManagedResource;
import cc.tonyhook.carambola.backend.entity.security.Permission;
import cc.tonyhook.carambola.backend.service.security.PermissionService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.transaction.Transactional;

@RestController
public class PermissionController {

    private final EntityManagerFactory emf;
    private final PermissionService permissionService;

    public PermissionController(EntityManagerFactory emf, PermissionService permissionService) {
        this.emf = emf;
        this.permissionService = permissionService;
    }

    @GetMapping(value = "/api/managed/permission/resourceType", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<String>> getResourceTypeList() {
        List<String> resourceTypeList = new ArrayList<String>();

        Metamodel mm = emf.getMetamodel();
        mm.getManagedTypes().forEach(entityType -> {
            if (ManagedResource.class.isAssignableFrom(entityType.getJavaType())) {
                if (!Modifier.isAbstract(entityType.getJavaType().getModifiers())) {
                    resourceTypeList.add(entityType.getJavaType().getSimpleName().toLowerCase());
                }
            }
        });

        return ResponseEntity.ok().body(resourceTypeList);
    }

    @GetMapping(value = "/api/managed/permission/resourceType/{resourceType}/resourceId/{resourceId}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Permission>> getItemPermissionList(
            @PathVariable String resourceType,
            @PathVariable Integer resourceId) {
        List<Permission> permissionList = permissionService.getItemPermissionList(resourceType, resourceId);

        return ResponseEntity.ok().body(permissionList);
    }

    @GetMapping(value = "/api/managed/permission/resourceType/{resourceType}/resourceId/{resourceId}/inherited", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Permission>> getInheritedPermissionList(
            @PathVariable String resourceType,
            @PathVariable Integer resourceId) {
        List<Permission> permissionList = permissionService.getInheritedPermissionList(resourceType, resourceId);

        return ResponseEntity.ok().body(permissionList);
    }

    @GetMapping(value = "/api/managed/permission/resourceType/{resourceType}/resourceId/{resourceId}/full", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Permission>> getFullPermissionList(
            @PathVariable String resourceType,
            @PathVariable Integer resourceId) {
        List<Permission> permissionList = permissionService.getFullPermissionList(resourceType, resourceId);

        return ResponseEntity.ok().body(permissionList);
    }

    @GetMapping(value = "/api/managed/permission/resourceType/{resourceType}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Permission>> getClassPermissionList(
            @PathVariable String resourceType) {
        List<Permission> permissionList = permissionService.getClassPermissionList(resourceType);

        return ResponseEntity.ok().body(permissionList);
    }

    @GetMapping(value = "/api/managed/permission/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Permission> getPermission(
            @PathVariable Integer id) {
        Permission permission = permissionService.getPermission(id);

        if (permission != null) {
            return ResponseEntity.ok().body(permission);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/api/managed/permission", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<Permission> addPermission(
            @RequestBody Permission newPermission) throws URISyntaxException {
        Permission updatedPermission = permissionService.addPermission(newPermission);

        return ResponseEntity
                .created(new URI("/api/managed/permission/" + updatedPermission.getId()))
                .body(updatedPermission);
    }

    @PutMapping(value = "/api/managed/permission/{id}", consumes = "application/json; charset=UTF-8")
    public ResponseEntity<?> updatePermission(
            @PathVariable Integer id,
            @RequestBody Permission newPermission) {
        if (!id.equals(newPermission.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Permission targetPermission = permissionService.getPermission(id);
        if (targetPermission == null) {
            return ResponseEntity.notFound().build();
        }

        permissionService.updatePermission(id, newPermission);

        return ResponseEntity.ok().build();
    }

    @Transactional
    @DeleteMapping("/api/managed/permission/{id}")
    public ResponseEntity<?> removePermission(
            @PathVariable Integer id) {
        Permission deletedPermission = permissionService.getPermission(id);
        if (deletedPermission == null) {
            return ResponseEntity.notFound().build();
        }

        permissionService.removePermission(id);

        return ResponseEntity.ok().build();
    }

}
