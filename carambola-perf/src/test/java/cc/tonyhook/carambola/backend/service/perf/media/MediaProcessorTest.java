package cc.tonyhook.carambola.backend.service.perf.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;

class MediaProcessorTest {

    private static final String SERVER = "https://perf.example.com/";

    private final PerfDebugPrintService debug = mock(PerfDebugPrintService.class);

    @Test
    void ownProtocolCarriesAmount() {
        MediaProcessorDavidia davidia = new MediaProcessorDavidia(debug, SERVER);

        assertThat(davidia.event(queries("davidia_id", "MC1", "davidia_event", EventCodes.APP_PAY, "davidia_amount", "1234")).getAmount())
            .isEqualByComparingTo(new BigDecimal("1234"));
        assertThat(davidia.event(queries("davidia_id", "MC1", "davidia_event", EventCodes.APP_PAY, "davidia_amount", "abc"))).isNull();
        assertThat(davidia.getEventUrl("MC1", EventCodes.CLICK))
            .startsWith("https://perf.example.com/api/open/event?davidia_media=davidia&davidia_id=MC1&davidia_event=0002&davidia_callback=__DAVIDIA_CALLBACK__&");
    }

    private static Map<String, String> queries(String... pairs) {
        Map<String, String> queries = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            queries.put(pairs[i], pairs[i + 1]);
        }
        return queries;
    }

}
