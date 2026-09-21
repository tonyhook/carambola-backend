package cc.tonyhook.carambola.backend.entity.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void callbackUrlIsKeptInMemoryButNotStored() {
        Event event = new Event();
        event.setQueries(queries("oaid", "O1", Event.CALLBACK_QUERY, "https://media.example.com/cb?t=1"));

        event.storeQueries();

        assertThat(event.getQueries()).containsEntry(Event.CALLBACK_QUERY, "https://media.example.com/cb?t=1");
        event.loadQueries();
        assertThat(event.getQueries()).containsEntry("oaid", "O1").doesNotContainKey(Event.CALLBACK_QUERY);
    }

    @Test
    void storingDoesNotAliasTheInMemoryQueries() {
        Event event = new Event();
        event.setQueries(queries("oaid", "O1"));

        event.storeQueries();
        event.getQueries().put("ip", "1.2.3.4");
        event.loadQueries();

        assertThat(event.getQueries()).doesNotContainKey("ip");
    }

    @Test
    void missingQueriesStayMissing() {
        Event event = new Event();

        event.storeQueries();
        event.loadQueries();

        assertThat(event.getQueries()).isNull();
    }

    private static Map<String, String> queries(String... keyValues) {
        Map<String, String> queries = new LinkedHashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            queries.put(keyValues[i], keyValues[i + 1]);
        }
        return queries;
    }

}
