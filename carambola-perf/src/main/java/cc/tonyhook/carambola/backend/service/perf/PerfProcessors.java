package cc.tonyhook.carambola.backend.service.perf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cc.tonyhook.carambola.backend.service.perf.media.MediaProcessor;
import cc.tonyhook.carambola.backend.service.perf.track.TrackProcessor;

/**
 * 按名字(davidia_media、davidia_track、渠道的媒体名、路由的监测方名)找处理器。
 * 名字即各处理器构造时声明的 name,首字母大小写不敏感。
 */
@Component
public class PerfProcessors {

    private final Map<String, MediaProcessor> mediaProcessors = new HashMap<String, MediaProcessor>();

    private final Map<String, TrackProcessor> trackProcessors = new HashMap<String, TrackProcessor>();

    public PerfProcessors(List<MediaProcessor> mediaProcessors, List<TrackProcessor> trackProcessors) {
        for (MediaProcessor mediaProcessor : mediaProcessors) {
            register(this.mediaProcessors, mediaProcessor.getName(), mediaProcessor);
        }
        for (TrackProcessor trackProcessor : trackProcessors) {
            register(this.trackProcessors, trackProcessor.getName(), trackProcessor);
        }
    }

    public MediaProcessor media(String name) {
        return mediaProcessors.get(key(name));
    }

    public TrackProcessor track(String name) {
        return trackProcessors.get(key(name));
    }

    private static <T> void register(Map<String, T> processors, String name, T processor) {
        if (processors.putIfAbsent(key(name), processor) != null) {
            throw new IllegalStateException("Duplicate perf processor name: " + name);
        }
    }

    private static String key(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

}
