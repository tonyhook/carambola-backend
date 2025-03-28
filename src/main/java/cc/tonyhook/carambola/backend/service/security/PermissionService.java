package cc.tonyhook.carambola.backend.service.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.security.PermissionRepository;
import cc.tonyhook.carambola.backend.entity.ContainedManagedResource;
import cc.tonyhook.carambola.backend.entity.HierarchyManagedResource;
import cc.tonyhook.carambola.backend.entity.ManagedResource;
import cc.tonyhook.carambola.backend.entity.security.Permission;
import jakarta.transaction.Transactional;

@Service
public class PermissionService {

    private final ApplicationContext applicationContext;
    private final PermissionRepository permissionRepository;

    public PermissionService(ApplicationContext applicationContext, PermissionRepository permissionRepository) {
        this.applicationContext = applicationContext;
        this.permissionRepository = permissionRepository;
    }

    @SuppressWarnings("unchecked")
    private ManagedResource getResourceById(String resourceType, Integer resourceId) {
        Object rawRepository = applicationContext.getBean(resourceType + "Repository");
        CrudRepository<? extends ManagedResource, Integer> repository = (CrudRepository<? extends ManagedResource, Integer>) rawRepository;

        return repository.findById(resourceId).orElse(null);
    }

    private List<Permission> getItemPermissionListInternal(String resourceType, Integer resourceId) {
        List<Permission> itemPermissionList = permissionRepository.findByResourceTypeAndResourceId(resourceType, resourceId);

        return itemPermissionList;
    }

    private List<Permission> getInheritedPermissionListInternal(String resourceType, Integer resourceId) {
        List<Permission> inheritedPermissionList = new ArrayList<Permission>();

        List<Permission> itemPermissionList = permissionRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
        Boolean inherited = false;
        for (Permission permission : itemPermissionList) {
            if (permission.getPermission() == null) {
                inherited = true;
            }
        }

        if (inherited) {
            ManagedResource r = getResourceById(resourceType, resourceId);

            if (r != null) {
                if (r instanceof HierarchyManagedResource) {
                    // hierarchy resource
                    if (((HierarchyManagedResource) r).getParentId() != null) {
                        // non-top level, get parent permission
                        inheritedPermissionList = getFullPermissionListInternal(resourceType, ((HierarchyManagedResource) r).getParentId());
                    } else {
                        // top level, get class permission
                        inheritedPermissionList = permissionRepository.findByResourceTypeAndResourceIdIsNull(resourceType);
                    }
                } else if (r instanceof ContainedManagedResource) {
                    // contained resource
                    String containerType = ((ContainedManagedResource) r).getContainerType();
                    Integer containerId = ((ContainedManagedResource) r).getContainerId();
                    if ((containerType != null) && (containerId != null)) {
                        // contained, get container permission
                        inheritedPermissionList = getFullPermissionListInternal(containerType, containerId);
                    } else {
                        // orphan, get class permission
                        inheritedPermissionList = permissionRepository.findByResourceTypeAndResourceIdIsNull(resourceType);
                    }
                } else {
                    // flat resource, get class permission
                    inheritedPermissionList = permissionRepository.findByResourceTypeAndResourceIdIsNull(resourceType);
                }
            }
        }

        return inheritedPermissionList;
    }

    private List<Permission> getFullPermissionListInternal(String resourceType, Integer resourceId) {
        List<Permission> permissionList = new ArrayList<Permission>();

        List<Permission> itemPermissionList = getItemPermissionListInternal(resourceType, resourceId);
        List<Permission> inheritedPermissionList = getInheritedPermissionListInternal(resourceType, resourceId);

        Iterator<Permission> iterator = itemPermissionList.iterator();
        while (iterator.hasNext()) {
            Permission permission = iterator.next();
            if (permission.getPermission() == null) {
                iterator.remove();
            }
        }

        permissionList.addAll(itemPermissionList);
        permissionList.addAll(inheritedPermissionList);

        Map<Integer, Set<String>> map = new HashMap<Integer, Set<String>>();
        for (Permission permission : permissionList) {
            Set<String> ops;
            if (map.containsKey(permission.getRoleId())) {
                ops = map.get(permission.getRoleId());
                for (int i = 0; i < permission.getPermission().length(); i++) {
                    String op = permission.getPermission().substring(i, i + 1);
                    ops.add(op);
                }
            } else {
                ops = new HashSet<String>();
                for (int i = 0; i < permission.getPermission().length(); i++) {
                    String op = permission.getPermission().substring(i, i + 1);
                    ops.add(op);
                }
                map.put(permission.getRoleId(), ops);
            }
        }

        permissionList = new ArrayList<Permission>();
        for (Integer roleId : map.keySet()) {
            Set<String> ops = map.get(roleId);
            String allowOps = "";

            for (String op : ops) {
                allowOps += op;
            }

            Permission permission = new Permission();
            permission.setResourceType(resourceType);
            permission.setResourceId(resourceId);
            permission.setRoleId(roleId);
            permission.setPermission(allowOps);

            permissionList.add(permission);
        }

        return permissionList;
    }

    private List<Permission> getClassPermissionListInternal(String resourceType) {
        List<Permission> classPermissionList = permissionRepository.findByResourceTypeAndResourceIdIsNull(resourceType);

        return classPermissionList;
    }

    @PreAuthorize("hasPermission(#resourceId, #resourceType, 'r') or hasAuthority('SECURITY_MANAGEMENT')")
    public List<Permission> getItemPermissionList(String resourceType, Integer resourceId) {
        if (resourceType == null) {
            return new ArrayList<Permission>();
        }
        if (resourceId == null) {
            return new ArrayList<Permission>();
        }

        return getItemPermissionListInternal(resourceType, resourceId);
    }

    @PreAuthorize("hasPermission(#resourceId, #resourceType, 'r') or hasAuthority('SECURITY_MANAGEMENT')")
    public List<Permission> getInheritedPermissionList(String resourceType, Integer resourceId) {
        if (resourceType == null) {
            return new ArrayList<Permission>();
        }
        if (resourceId == null) {
            return new ArrayList<Permission>();
        }

        return getInheritedPermissionListInternal(resourceType, resourceId);
    }

    @PreAuthorize("hasPermission(#resourceId, #resourceType, 'r') or hasAuthority('SECURITY_MANAGEMENT')")
    public List<Permission> getFullPermissionList(String resourceType, Integer resourceId) {
        if (resourceType == null) {
            return new ArrayList<Permission>();
        }
        if (resourceId == null) {
            return new ArrayList<Permission>();
        }

        return getFullPermissionListInternal(resourceType, resourceId);
    }

    @PreAuthorize("hasPermission(null, #resourceType, 'r') or hasAuthority('SECURITY_MANAGEMENT')")
    public List<Permission> getClassPermissionList(String resourceType) {
        if (resourceType == null) {
            return new ArrayList<Permission>();
        }

        return getClassPermissionListInternal(resourceType);
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public Permission getPermission(Integer id) {
        Permission permission = permissionRepository.findById(id).orElse(null);

        return permission;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public Permission addPermission(Permission newPermission) {
        Permission updatedPermission = permissionRepository.save(newPermission);

        return updatedPermission;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public void updatePermission(Integer id, Permission newPermission) {
        permissionRepository.save(newPermission);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public void removePermission(Integer id) {
        Permission deletedPermission = permissionRepository.findById(id).orElse(null);

        permissionRepository.delete(deletedPermission);
    }

}
