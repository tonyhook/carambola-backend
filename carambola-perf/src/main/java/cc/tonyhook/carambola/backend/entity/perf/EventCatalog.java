package cc.tonyhook.carambola.backend.entity.perf;

import java.util.List;

public final class EventCatalog {

    private EventCatalog() {
    }

    public record EventInfo(String category, String name) {
    }

    public record EventOption(String event, String category, String name) {
    }

    public record EventOptions(List<EventOption> events, String defaultEvent) {
    }

}
