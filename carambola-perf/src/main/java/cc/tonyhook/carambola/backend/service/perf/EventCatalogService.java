package cc.tonyhook.carambola.backend.service.perf;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.EventCatalogRepository;
import cc.tonyhook.carambola.backend.entity.perf.EventCatalog;
import cc.tonyhook.carambola.backend.entity.perf.EventCatalogEntry;

@Service
public class EventCatalogService {

    private final EventCatalogRepository eventCatalogRepository;

    public EventCatalogService(EventCatalogRepository eventCatalogRepository) {
        this.eventCatalogRepository = eventCatalogRepository;
    }

    public EventCatalog.EventInfo get(String event) {
        EventCatalogEntry entry = eventCatalogRepository.findFirstByEvent(event);
        if (entry != null && !Boolean.TRUE.equals(entry.getDeleted())) {
            return new EventCatalog.EventInfo(entry.getCategory(), entry.getName());
        }

        return new EventCatalog.EventInfo("", "");
    }

    public EventCatalog.EventOptions getMediaEvents() {
        return new EventCatalog.EventOptions(
            eventCatalogRepository.findByPairingEventTrueAndDeletedFalseOrderByEventAsc().stream()
                .map(entry -> new EventCatalog.EventOption(entry.getEvent(), entry.getCategory(), entry.getName()))
                .toList(),
            getDefaultMediaEvent());
    }

    public String getDefaultMediaEvent() {
        EventCatalogEntry entry = eventCatalogRepository.findFirstByDefaultMediaEventTrueAndDeletedFalseOrderByEventAsc();
        if (entry != null) {
            return entry.getEvent();
        }

        return "";
    }

}
