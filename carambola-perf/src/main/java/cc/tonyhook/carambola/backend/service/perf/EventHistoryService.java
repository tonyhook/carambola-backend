package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.Collection;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.EventRepository;
import cc.tonyhook.carambola.backend.entity.perf.Event;

// 回溯同一个入口事件此前发生过的转化。只依赖仓储,媒体处理器可以直接注入它:
// EventService 自身注入了 MediaProcessor 的集合,处理器再注入 EventService 会成环。
@Service
public class EventHistoryService {

    private final EventRepository eventRepository;

    public EventHistoryService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // 同一个入口事件下,给定这几个事件里最早发生的那次的时间;一次都没有则为 null
    public Timestamp getFirstCallbackTime(Event event, Collection<String> events) {
        if ((event == null) || (event.getId() == null) || (events == null) || events.isEmpty()) {
            return null;
        }

        return eventRepository.findFirstCallbackTime(event.getId(), events);
    }

}
