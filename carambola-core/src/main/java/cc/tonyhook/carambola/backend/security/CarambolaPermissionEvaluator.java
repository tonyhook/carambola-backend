package cc.tonyhook.carambola.backend.security;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cc.tonyhook.carambola.backend.dao.security.PermissionRepository;
import cc.tonyhook.carambola.backend.dao.security.RoleRepository;
import cc.tonyhook.carambola.backend.dao.security.UserRepository;
import cc.tonyhook.carambola.backend.entity.ContainedManagedResource;
import cc.tonyhook.carambola.backend.entity.HierarchyManagedResource;
import cc.tonyhook.carambola.backend.entity.ManagedResource;
import cc.tonyhook.carambola.backend.entity.security.Permission;
import cc.tonyhook.carambola.backend.entity.security.Role;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CarambolaPermissionEvaluator implements PermissionEvaluator {

    private final ApplicationContext applicationContext;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public CarambolaPermissionEvaluator(
            ApplicationContext applicationContext,
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository
    ) {
        this.applicationContext = applicationContext;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    /*
        Class Permission Rules

        resourceType    resourceId  role    permission  rule
        ------------    ----------  ----    ----------  ---------------------------------------
        T               null        R       P           R has P for class of T
    */

    Set<String> getClassPermission(Integer roleId, String resourceType) {
        Set<String> allowedOps = new HashSet<String>();

        if (resourceType == null) {
            return allowedOps;
        }

        List<Permission> permissions = permissionRepository.findByResourceTypeAndResourceIdAndRoleId(resourceType, null, roleId);
        for (Permission permission : permissions) {
            String ops = permission.getPermission();
            for (int i = 0; i < ops.length(); i++) {
                String op = ops.substring(i, i + 1);
                allowedOps.add(op);
            }
        }
        return allowedOps;
    }

    /*
        Item Permission Rules

        resourceType    resourceId  role    permission  rule
        ------------    ----------  ----    ----------  ---------------------------------------------------------------------
        T               I           null    null        I inherits permissions from it's parent (HierarchyManagedResource)
        T               I           null    null        I inherits permissions from it's container (ContainedManagedResource)
        T               I           R       P           R has P for I
    */
    Set<String> getItemPermission(Integer roleId, String resourceType, Integer resourceId) {
        Set<String> allowedOps = new HashSet<String>();

        if (resourceType == null) {
            return allowedOps;
        }
        if (resourceId == null) {
            return allowedOps;
        }

        List<Permission> itemPermission = permissionRepository.findByResourceTypeAndResourceIdAndRoleId(resourceType, resourceId, roleId);
        for (Permission permission : itemPermission) {
            String ops = permission.getPermission();
            if (ops != null) {
                // regular permission
                for (int i = 0; i < ops.length(); i++) {
                    String op = ops.substring(i, i + 1);
                    allowedOps.add(op);
                }
            }
        }

        List<Permission> inheritedPermission = permissionRepository.findByResourceTypeAndResourceIdAndRoleId(resourceType, resourceId, null);
        for (Permission permission : inheritedPermission) {
            String ops = permission.getPermission();
            if (ops == null) {
                // inherited permission
                ManagedResource r = getResourceById(resourceType, resourceId);

                if (r != null) {
                    if (r instanceof HierarchyManagedResource) {
                        // hierarchy resource
                        if (((HierarchyManagedResource) r).getParentId() != null) {
                            // non-top level, get parent permission
                            Set<String> inheritedAllowedOps = getItemPermission(roleId, resourceType, ((HierarchyManagedResource) r).getParentId());
                            allowedOps.addAll(inheritedAllowedOps);
                        } else {
                            // top level, get class permission
                            Set<String> inheritedAllowedOps = getClassPermission(roleId, resourceType);
                            allowedOps.addAll(inheritedAllowedOps);
                        }
                    } else if (r instanceof ContainedManagedResource) {
                        // contained resource
                        if ((((ContainedManagedResource) r).getContainerType() != null) && (((ContainedManagedResource) r).getContainerId() != null)) {
                            // contained, get container permission
                            Set<String> inheritedAllowedOps = getItemPermission(roleId, ((ContainedManagedResource) r).getContainerType(), ((ContainedManagedResource) r).getContainerId());
                            allowedOps.addAll(inheritedAllowedOps);
                        } else {
                            // orphen, get class permission
                            Set<String> inheritedAllowedOps = getClassPermission(roleId, resourceType);
                            allowedOps.addAll(inheritedAllowedOps);
                        }
                    } else {
                        // flat resource, get class permission
                        Set<String> inheritedAllowedOps = getClassPermission(roleId, resourceType);
                        allowedOps.addAll(inheritedAllowedOps);
                    }
                }
            }
        }

        return allowedOps;
    }

    private Set<Role> getRoles(Authentication authentication) {
        if (authentication.getPrincipal() instanceof String) {
            // anonymous user
            Set<Role> roles = new HashSet<Role>();
            roles.add(roleRepository.findById(0).orElse(null));
            return roles;
        } else {
            // authenticated user
            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            Set<Role> roles = userRepository.findByUsername(username).getRoles();
            return roles;
        }
    }

    @SuppressWarnings("unchecked")
    private ManagedResource getResourceById(String resourceType, Integer resourceId) {
        Object rawRepository = applicationContext.getBean(resourceType + "Repository");
        CrudRepository<? extends ManagedResource, Integer> repository = (CrudRepository<? extends ManagedResource, Integer>) rawRepository;

        return repository.findById(resourceId).orElse(null);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (!(targetDomainObject instanceof ManagedResource)) {
            return true;
        }

        ManagedResource r = (ManagedResource) targetDomainObject;
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            return false;
        }

        // owner has full control
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        if (r.getOwnerId() != null && r.getOwnerId().equals((Integer) request.getSession().getAttribute("id"))) {
            return true;
        }

        String op = (String) permission;
        Set<Role> roles = getRoles(authentication);
        Set<String> allowedOps = new HashSet<String>();

        String resourceFullType = r.getClass().getName();
        String resourceType = resourceFullType.split("\\.")[resourceFullType.split("\\.").length - 1].toLowerCase();

        for (Role role : roles) {
            if (op.equals("c")) {
                // create resource should check parant/class permission
                if (targetDomainObject instanceof HierarchyManagedResource) {
                    // hierarchy resource, check parant permission
                    Integer parentId = ((HierarchyManagedResource) targetDomainObject).getParentId();
                    if (parentId != null) {
                        // non-top level, get parent permission
                        allowedOps.addAll(getItemPermission(role.getId(), resourceType, parentId));
                    } else {
                        // top level, get class permission
                        allowedOps.addAll(getClassPermission(role.getId(), resourceType));
                    }
                } else if (targetDomainObject instanceof ContainedManagedResource) {
                    // contained resource, check container permission
                    String containerType = ((ContainedManagedResource) targetDomainObject).getContainerType();
                    Integer containerId = ((ContainedManagedResource) targetDomainObject).getContainerId();
                    if ((containerType != null) && (containerId != null)) {
                        // contained, get container permission
                        allowedOps.addAll(getItemPermission(role.getId(), containerType, containerId));
                    } else {
                        // orphan, get class permission
                        allowedOps.addAll(getClassPermission(role.getId(), resourceType));
                    }
                } else {
                    // flat resource, check class permission
                    allowedOps.addAll(getClassPermission(role.getId(), resourceType));
                }
            } else {
                // read/update/delete resource should check self permission
                allowedOps.addAll(getItemPermission(role.getId(), resourceType, r.getId()));
            }
        }

        return allowedOps.contains(op);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (targetId != null) {
            ManagedResource r = getResourceById(targetType, (Integer) targetId);

            return hasPermission(authentication, r, permission);
        } else {
            String op = (String) permission;
            Set<Role> roles = getRoles(authentication);
            Set<String> allowedOps = new HashSet<String>();

            for (Role role : roles) {
                allowedOps.addAll(getClassPermission(role.getId(), targetType));
            }

            return allowedOps.contains(op);
        }
    }

}
