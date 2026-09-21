package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.EventCallbackUrlRepository;
import cc.tonyhook.carambola.backend.dao.perf.EventRepository;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.EventPageView;
import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCallbackUrl;
import cc.tonyhook.carambola.backend.entity.perf.EventView;
import cc.tonyhook.carambola.backend.service.perf.media.MediaProcessor;
import jakarta.transaction.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventCallbackUrlRepository eventCallbackUrlRepository;
    private final ClientChannelService clientChannelService;
    private final EventCatalogService eventCatalogService;
    private final PerfProcessors perfProcessors;

    public EventService(
            EventRepository eventRepository,
            EventCallbackUrlRepository eventCallbackUrlRepository,
            ClientChannelService clientChannelService,
            EventCatalogService eventCatalogService,
            PerfProcessors perfProcessors
    ) {
        this.eventRepository = eventRepository;
        this.eventCallbackUrlRepository = eventCallbackUrlRepository;
        this.clientChannelService = clientChannelService;
        this.eventCatalogService = eventCatalogService;
        this.perfProcessors = perfProcessors;
    }

    public List<Event> getEventList() {
        List<Event> eventList = eventRepository.findAll();

        return eventList;
    }

    public Event getEvent(Integer id) {
        Event event = eventRepository.findById(id).orElse(null);

        return event;
    }

    @Transactional
    public Event addEvent(Event newEvent) {
        if (newEvent.getTime() == null) {
            newEvent.setTime(new Timestamp(System.currentTimeMillis()));
        }
        if (newEvent.getDirection() == null) {
            newEvent.setDirection(Event.DIRECTION_UPSTREAM);
        }
        Event updatedEvent = eventRepository.save(newEvent);
        saveCallbackUrl(updatedEvent);

        return updatedEvent;
    }

    @Transactional
    public Event addCallback(Event callback) {
        if (callback.getTime() == null) {
            callback.setTime(new Timestamp(System.currentTimeMillis()));
        }
        callback.setDirection(Event.DIRECTION_DOWNSTREAM);
        callback = eventRepository.save(callback);
        saveCallbackUrl(callback);

        return callback;
    }

    // 回传地址不随 perf_event 落库(见 Event.storedQueries),插入主表的同一个事务里另存一份
    private void saveCallbackUrl(Event event) {
        String url = PerfQueries.get(event.getQueries(), Event.CALLBACK_QUERY);
        if (url != null) {
            eventCallbackUrlRepository.save(new EventCallbackUrl(event.getId(), url));
        }
    }

    // 从库里取出的入口事件不带回传地址,回传前从侧表补回 queries。
    // 旧行的地址还在 queries 列里,已经带着了;过了保留期的侧表行已删,补不回来,照旧按没有地址处理
    public void loadCallbackUrl(Event event) {
        if ((event == null) || (event.getId() == null)
            || PerfQueries.isValid(event.getQueries(), Event.CALLBACK_QUERY)) {
            return;
        }

        EventCallbackUrl callbackUrl = eventCallbackUrlRepository.findById(event.getId()).orElse(null);
        if (callbackUrl == null) {
            return;
        }
        if (event.getQueries() == null) {
            event.setQueries(new LinkedHashMap<String, String>());
        }
        event.getQueries().put(Event.CALLBACK_QUERY, callbackUrl.getUrl());
    }

    // 回传地址只在上游凭 delivery token 找回入口事件时才用得上,而 token 只随发出的请求离开。
    // 一个请求都没发出的事件永远不会被回传,它的地址从写入起就是死数据,不必存。
    // 入库前调用只是从 queries 里拿掉,入库后调用还要删掉侧表那一行
    public void dropCallbackUrl(Event event) {
        if (event.getQueries() != null) {
            event.getQueries().remove(Event.CALLBACK_QUERY);
        }
        if (event.getId() != null) {
            eventCallbackUrlRepository.deleteById(event.getId());
        }
    }

    public Event updateEvent(Event event) {
        return eventRepository.save(event);
    }

    public EventPageView getRecentEvents(
            org.springframework.security.core.Authentication authentication,
            Timestamp start,
            Timestamp end,
            String event,
            Integer clientId,
            Integer clientProjectId,
            Integer clientChannelId,
            String media,
            String operator,
            Integer page,
            Integer size
    ) {
        Set<Integer> qualifiedClientChannelIds = new HashSet<Integer>();
        for (ClientChannel clientChannel : clientChannelService.getClientChannelList(authentication)) {
            if (matchesFilters(clientChannel, clientId, clientProjectId, clientChannelId, media, operator)) {
                qualifiedClientChannelIds.add(clientChannel.getId());
            }
        }
        EventPageView pageView = new EventPageView();
        pageView.setPage(page);
        pageView.setSize(size);
        if (qualifiedClientChannelIds.isEmpty()) {
            pageView.setContent(List.of());
            pageView.setTotalElements(0L);
            return pageView;
        }

        int offset = page * size;
        List<Event> events = eventRepository.findRecentEvents(
            start,
            end,
            event,
            qualifiedClientChannelIds,
            size,
            offset);
        Long total = eventRepository.countRecentEvents(
            start,
            end,
            event,
            qualifiedClientChannelIds);

        List<EventView> eventViews = new ArrayList<EventView>();
        for (Event currentEvent : events) {
            eventViews.add(toEventView(currentEvent));
        }

        pageView.setContent(eventViews);
        pageView.setTotalElements(total);
        return pageView;
    }

    private boolean matchesFilters(
            ClientChannel clientChannel,
            Integer clientId,
            Integer clientProjectId,
            Integer clientChannelId,
            String media,
            String operator
    ) {
        if (clientId != null && (clientChannel.getClient() == null || !clientId.equals(clientChannel.getClient().getId()))) {
            return false;
        }
        if (clientProjectId != null && (clientChannel.getClientProject() == null || !clientProjectId.equals(clientChannel.getClientProject().getId()))) {
            return false;
        }
        if (clientChannelId != null && !clientChannelId.equals(clientChannel.getId())) {
            return false;
        }
        if (media != null && !media.equals(clientChannel.getMediaName())) {
            return false;
        }
        String operatorValue = clientChannel.getOperator();
        if (operatorValue == null || operatorValue.isBlank()) {
            operatorValue = clientChannel.getMediaName();
        }
        if (operator != null && !operator.equals(operatorValue)) {
            return false;
        }

        return true;
    }

    private EventView toEventView(Event event) {
        EventView view = new EventView();
        view.setId(event.getId());
        view.setTime(event.getTime());
        view.setEvent(event.getEvent());
        view.setEventName(eventCatalogService.get(event.getEvent()).name());
        view.setAmount(event.getAmount());
        if (event.getClientChannel() != null) {
            view.setMedia(event.getClientChannel().getMediaName());
            view.setMediaCode(event.getClientChannel().getMediaCode());
            view.setOperator(event.getClientChannel().getOperator());
        }
        view.setForwarded(event.getForwarded());
        view.setForwardSucceeded(event.getForwardSucceeded());
        view.setQueries(cleanQueries(event, displayQueries(event)));

        return view;
    }

    private Map<String, String> displayQueries(Event event) {
        if (!Event.DIRECTION_DOWNSTREAM.equals(event.getDirection())) {
            return event.getQueries();
        }

        Event upstreamEvent = getUpstreamEvent(event);
        if (upstreamEvent == null || upstreamEvent.getQueries() == null) {
            return event.getQueries();
        }

        Map<String, String> queries = new LinkedHashMap<String, String>();
        queries.putAll(upstreamEvent.getQueries());
        if (event.getQueries() != null) {
            queries.putAll(event.getQueries());
        }

        return queries;
    }

    private Event getUpstreamEvent(Event event) {
        if (event == null) {
            return null;
        }
        return event.getEventDelivery() == null ? null : event.getEventDelivery().getEvent();
    }

    private Map<String, String> cleanQueries(Event event, Map<String, String> queries) {
        ClientChannel clientChannel = event.getClientChannel();
        if (clientChannel == null || clientChannel.getMediaName() == null) {
            return queries;
        }

        MediaProcessor mediaProcessor = perfProcessors.media(clientChannel.getMediaName());
        if (mediaProcessor == null) {
            return queries;
        }

        return mediaProcessor.cleanQueries(queries);
    }

}
