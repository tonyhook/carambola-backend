package cc.tonyhook.carambola.backend.dao.security;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.security.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    List<Permission> findByResourceTypeAndResourceId(String resourceType, Integer resourceId);
    List<Permission> findByResourceTypeAndResourceIdIsNull(String resourceType);
    List<Permission> findByResourceTypeAndResourceIdAndRoleId(String resourceType, Integer resourceId, Integer roleId);
    List<Permission> deleteByResourceTypeAndResourceId(String resourceType, Integer resourceId);
    List<Permission> deleteByRoleId(Integer roleId);

}
