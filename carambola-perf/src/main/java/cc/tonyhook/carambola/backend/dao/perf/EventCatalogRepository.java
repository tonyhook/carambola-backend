package cc.tonyhook.carambola.backend.dao.perf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.EventCatalogEntry;

public interface EventCatalogRepository extends JpaRepository<EventCatalogEntry, Integer> {

    EventCatalogEntry findFirstByEvent(String event);

    List<EventCatalogEntry> findByDeletedFalseOrderByEventAsc();

    List<EventCatalogEntry> findByPairingEventTrueAndDeletedFalseOrderByEventAsc();

    EventCatalogEntry findFirstByDefaultMediaEventTrueAndDeletedFalseOrderByEventAsc();

}
