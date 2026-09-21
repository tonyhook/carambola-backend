package cc.tonyhook.carambola.backend.service.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import cc.tonyhook.carambola.backend.dao.perf.EventCallbackUrlRepository;
import cc.tonyhook.carambola.backend.dao.perf.EventRepository;
import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCallbackUrl;

class EventServiceTest {

    private static final String CALLBACK = "https://media.example.com/cb?t=1";

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventCallbackUrlRepository callbackUrlRepository = mock(EventCallbackUrlRepository.class);
    private final EventService eventService = new EventService(
        eventRepository,
        callbackUrlRepository,
        mock(ClientChannelService.class),
        mock(EventCatalogService.class),
        mock(PerfProcessors.class));

    @Test
    void callbackUrlIsSavedUnderTheNewEventId() {
        givenSaveAssignsId(42);
        Event event = event(CALLBACK);

        eventService.addEvent(event);

        ArgumentCaptor<EventCallbackUrl> saved = ArgumentCaptor.forClass(EventCallbackUrl.class);
        verify(callbackUrlRepository).save(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo(42);
        assertThat(saved.getValue().getUrl()).isEqualTo(CALLBACK);
        assertThat(event.getQueries()).containsEntry(Event.CALLBACK_QUERY, CALLBACK);
    }

    @Test
    void eventsWithoutCallbackUrlWriteNoSideRow() {
        givenSaveAssignsId(42);

        eventService.addEvent(event(null));
        eventService.addEvent(event("__DAVIDIA_CALLBACK__"));
        eventService.addCallback(event(null));

        verify(callbackUrlRepository, never()).save(any());
    }

    @Test
    void loadedEntryGetsItsCallbackUrlBack() {
        when(callbackUrlRepository.findById(42)).thenReturn(Optional.of(new EventCallbackUrl(42, CALLBACK)));
        Event entry = event(null);
        entry.setId(42);

        eventService.loadCallbackUrl(entry);

        assertThat(entry.getQueries()).containsEntry(Event.CALLBACK_QUERY, CALLBACK).containsEntry("oaid", "O1");
    }

    @Test
    void legacyRowsKeepTheirInlineCallbackUrl() {
        Event entry = event("https://media.example.com/legacy");
        entry.setId(42);

        eventService.loadCallbackUrl(entry);

        assertThat(entry.getQueries()).containsEntry(Event.CALLBACK_QUERY, "https://media.example.com/legacy");
        verify(callbackUrlRepository, never()).findById(any());
    }

    @Test
    void expiredCallbackUrlLeavesEntryWithoutOne() {
        when(callbackUrlRepository.findById(42)).thenReturn(Optional.empty());
        Event entry = new Event();
        entry.setId(42);

        eventService.loadCallbackUrl(entry);

        assertThat(entry.getQueries()).isNull();
    }

    @Test
    void droppingBeforeInsertOnlyTouchesQueries() {
        Event event = event(CALLBACK);

        eventService.dropCallbackUrl(event);

        assertThat(event.getQueries()).doesNotContainKey(Event.CALLBACK_QUERY);
        verify(callbackUrlRepository, never()).deleteById(any());
    }

    @Test
    void droppingAfterInsertDeletesTheSideRow() {
        Event event = event(CALLBACK);
        event.setId(42);

        eventService.dropCallbackUrl(event);

        assertThat(event.getQueries()).doesNotContainKey(Event.CALLBACK_QUERY);
        verify(callbackUrlRepository).deleteById(42);
    }

    private void givenSaveAssignsId(int id) {
        when(eventRepository.save(any())).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId(id);
            return saved;
        });
    }

    private static Event event(String callback) {
        Map<String, String> queries = new LinkedHashMap<String, String>();
        queries.put("oaid", "O1");
        if (callback != null) {
            queries.put(Event.CALLBACK_QUERY, callback);
        }
        Event event = new Event();
        event.setQueries(queries);
        return event;
    }

}
