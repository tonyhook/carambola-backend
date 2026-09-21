package cc.tonyhook.carambola.backend.service.perf;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.EventCatalogRepository;
import cc.tonyhook.carambola.backend.entity.perf.EventCatalogEntry;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;

@Service
public class EventCatalogConsistencyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventCatalogConsistencyService.class);

    private final EventCatalogRepository eventCatalogRepository;

    public EventCatalogConsistencyService(EventCatalogRepository eventCatalogRepository) {
        this.eventCatalogRepository = eventCatalogRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkEventCatalog() {
        Set<String> codeEvents = new TreeSet<String>(EventCodes.all());
        Set<String> catalogEvents = eventCatalogRepository.findByDeletedFalseOrderByEventAsc().stream()
            .map(EventCatalogEntry::getEvent)
            .filter(event -> event != null && !event.isBlank())
            .collect(Collectors.toCollection(TreeSet::new));

        Set<String> missingInCatalog = new TreeSet<String>(codeEvents);
        missingInCatalog.removeAll(catalogEvents);

        Set<String> missingInCode = new TreeSet<String>(catalogEvents);
        missingInCode.removeAll(codeEvents);
        missingInCode.removeIf(EventCodes::isCustom);

        if (missingInCatalog.isEmpty() && missingInCode.isEmpty()) {
            LOGGER.info("Event catalog consistency check passed: events={}", codeEvents.size());
            return;
        }

        if (!missingInCatalog.isEmpty()) {
            LOGGER.warn("Event codes missing in perf_event_catalog: {}", missingInCatalog);
        }
        if (!missingInCode.isEmpty()) {
            LOGGER.warn("perf_event_catalog events missing in EventCodes: {}", missingInCode);
        }
    }

}
