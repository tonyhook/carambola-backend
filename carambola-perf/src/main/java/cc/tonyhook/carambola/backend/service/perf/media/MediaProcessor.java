package cc.tonyhook.carambola.backend.service.perf.media;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cc.tonyhook.carambola.backend.entity.perf.Callback;
import cc.tonyhook.carambola.backend.entity.perf.Event;

public abstract class MediaProcessor {

    abstract public Event event(Map<String, String> queries);
    abstract public Boolean callback(Callback callback, Event event);

    abstract public String getEventUrl();

    protected Boolean isValid(Map<String, String> queries, String key) {
        if (!queries.containsKey(key)) {
            return false;
        }
        if (StringUtils.isBlank(queries.get(key))) {
            return false;
        }
        if (queries.get(key).equals("__" + StringUtils.toRootUpperCase(key) + "__")) {
            return false;
        }
        return true;
    }

}
