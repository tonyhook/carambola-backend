package cc.tonyhook.carambola.backend.service.perf.track;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cc.tonyhook.carambola.backend.entity.perf.Callback;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.Event;

public abstract class TrackProcessor {

    abstract public Callback callback(Map<String, String> queries);
    abstract public Boolean event(Event event, ClientChannel cc);

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
