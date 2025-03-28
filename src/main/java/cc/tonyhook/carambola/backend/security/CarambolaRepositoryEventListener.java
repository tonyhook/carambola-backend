package cc.tonyhook.carambola.backend.security;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import cc.tonyhook.carambola.backend.dao.security.PermissionRepository;
import cc.tonyhook.carambola.backend.entity.ManagedResource;
import cc.tonyhook.carambola.backend.entity.security.Permission;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CarambolaRepositoryEventListener
        implements PreInsertEventListener, PostInsertEventListener {

    private final PermissionRepository permissionRepository;

    public CarambolaRepositoryEventListener(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        // set ownerId no matter what was provided
        Object rawEntity = event.getEntity();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            return false;
        }

        if (RequestContextHolder.getRequestAttributes() != null && ManagedResource.class.isAssignableFrom(rawEntity.getClass())) {
            ManagedResource entity = (ManagedResource) rawEntity;

            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            entity.setOwnerId((Integer) request.getSession().getAttribute("id"));
        }

        return false;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        // add default inherited permission
        Object rawEntity = event.getEntity();

        if (ManagedResource.class.isAssignableFrom(rawEntity.getClass())) {
            ManagedResource entity = (ManagedResource) rawEntity;

            String resourceFullType = entity.getClass().getName();
            String resourceType = resourceFullType.split("\\.")[resourceFullType.split("\\.").length - 1].toLowerCase();

            Permission permission = new Permission();
            permission.setResourceType(resourceType);
            permission.setResourceId(entity.getId());
            permission.setRoleId(null);
            permission.setPermission(null);
            permissionRepository.save(permission);
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

}
