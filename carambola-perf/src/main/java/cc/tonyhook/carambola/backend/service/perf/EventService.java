package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.EventRepository;
import cc.tonyhook.carambola.backend.entity.perf.Event;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SerialnoService serialnoService;

    public EventService(EventRepository eventRepository, SerialnoService serialnoService) {
        this.eventRepository = eventRepository;
        this.serialnoService = serialnoService;
    }

    public List<Event> getEventList() {
        List<Event> eventList = eventRepository.findAll();

        return eventList;
    }

    public Event getEvent(Integer id) {
        Event event = eventRepository.findById(id).orElse(null);

        return event;
    }

    public Event getEventBySerialno(String serialno) {
        Event event = eventRepository.findFirstBySerialno(serialno);

        return event;
    }

    public Event addEvent(Event newEvent) {
        if (newEvent.getTime() == null) {
            newEvent.setTime(new Timestamp(System.currentTimeMillis()));
        }
        if (newEvent.getSerialno() == null) {
            newEvent.setSerialno(serialnoService.nextSerialno());
        }

        Event updatedEvent = eventRepository.save(newEvent);

        return updatedEvent;
    }

}
