package cc.tonyhook.carambola.backend.service.perf.media;

import java.util.Map;

import org.springframework.stereotype.Component;

import cc.tonyhook.carambola.backend.entity.perf.Callback;
import cc.tonyhook.carambola.backend.entity.perf.Event;

@Component("mediaSina")
public class MediaProcessorSina extends MediaProcessor {

    @Override
    public Event event(Map<String, String> queries) {
        // 提供转化ID

        Event e = new Event();
        e.setMedia("sina");
        e.setMediaCode(null);
        e.setQueries(queries);
        return e;
    }

    @Override
    public Boolean callback(Callback callback, Event event) {
        return true;
    }

    @Override
    public String getEventUrl() {
        return "";
    }

}
