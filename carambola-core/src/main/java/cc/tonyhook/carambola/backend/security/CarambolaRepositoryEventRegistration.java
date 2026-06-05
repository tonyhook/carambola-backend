package cc.tonyhook.carambola.backend.security;

import org.hibernate.Session;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

@Component
public class CarambolaRepositoryEventRegistration {

    private final EntityManager entityManager;
    private final CarambolaRepositoryEventListener listener;

    public CarambolaRepositoryEventRegistration(EntityManager entityManager, CarambolaRepositoryEventListener listener) {
        this.entityManager = entityManager;
        this.listener = listener;
    }

    @PostConstruct
    private void registerListeners() {
        // create transactional EntityManager
        EntityManager entityManager1 = entityManager.getEntityManagerFactory().createEntityManager();

        final EventListenerRegistry registry = ((SessionFactoryImpl) entityManager1.unwrap(Session.class).getSessionFactory())
                .getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.PRE_INSERT)
                .appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_INSERT)
                .appendListener(listener);
    }

}
