package cc.tonyhook.carambola.backend.service.perf.track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;
import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;

class TrackProcessorTest {

    private static final String SERVER = "https://perf.example.com/";
    private static final String CALLBACK = "https://perf.example.com/api/open/callback?davidia_track=%s&davidia_delivery=tok";

    private final PerfDebugPrintService debug = mock(PerfDebugPrintService.class);

    @Test
    void kabossUsesPlatformIdWhenTraceIdMissing() {
        Map<String, String> params = entryParams(new TrackProcessorKaboss(debug, SERVER), EventCodes.CLICK, "ADM1", ios());

        assertThat(params).containsEntry("bchannel", "ADM1")
            .containsEntry("behavior", "CLICK")
            .containsEntry("trace_id", "7")
            .containsEntry("os", "ios")
            .containsEntry("caid", "[{\"caid\":\"c1\",\"version\":\"20250325\"}]")
            .containsEntry("client_ip", "240e::1")
            .containsEntry("plan_id", "");
    }

    @Test
    void ifengTakesHostFromTrackCode() {
        TrackProcessorIfeng ifeng = new TrackProcessorIfeng(debug, SERVER);
        Map<String, String> params = entryParams(ifeng, EventCodes.IMPRESSION, "c|g|a|m|ch|https://ifeng.example.com/", ios());

        assertThat(params.get("<path>")).isEqualTo("https://ifeng.example.com/d/imp");
        assertThat(params).containsEntry("channel", "ch")
            .containsEntry("os", "1")
            .containsEntry("caid", "20250325_c1");

        // 没配域名的旧路由按配置错误处理,不再发往“__BASE_URL__”
        assertThatThrownBy(() -> ifeng.buildEntryUrl(event(EventCodes.CLICK, android()), route("c|g|a|m|ch"), "tok"))
            .isInstanceOf(TrackProcessor.MisconfiguredRouteException.class);
    }

    @Test
    void ainianFollowsProtocolForOsAndCaid() {
        Map<String, String> params = entryParams(new TrackProcessorAinian(debug, SERVER), EventCodes.CLICK, "10001", ios());

        assertThat(params).containsEntry("oid", "10001")
            .containsEntry("rt", "2")
            .containsEntry("dot", "1")
            .containsEntry("cid", "")
            .containsEntry("caid", "[{\"caid\":\"c1\",\"version\":\"20250325\"}]");

        Map<String, String> harmony = entryParams(new TrackProcessorAinian(debug, SERVER), EventCodes.CLICK, "10001", Map.of("os", "HarmonyOS"));
        assertThat(harmony).containsEntry("dot", "3");
    }

    @Test
    void davidiaDropsUnresolvedTemplateParamsAndPassesTheRestThrough() {
        Map<String, String> params = entryParams(new TrackProcessorDavidia(debug, SERVER), EventCodes.CLICK,
            "https://t.example.com/c?id=__DAVIDIA_ID__&i=__IDFA__|D9", android());

        assertThat(params).containsEntry("id", "D9")
            .doesNotContainKey("i")
            .containsEntry("davidia_event", EventCodes.CLICK)
            .containsEntry("davidia_callback", CALLBACK.formatted("davidia"))
            .containsEntry("oaid", "O1");
    }

    @Test
    void haoyouAppendsDeviceParamsAndDeliveryToken() {
        Map<String, String> params = entryParams(new TrackProcessorHaoyou(debug, SERVER), EventCodes.CLICK,
            "https://h.example.com/click/m|1?game_id=1", android());

        // 只有一段的 trackCode 不按竖线拆
        assertThat(params.get("<path>")).isEqualTo("https://h.example.com/click/m%7C1");
        assertThat(params).containsEntry("game_id", "1")
            .containsEntry("oaid", "O1")
            .containsEntry("davidia_track", "haoyou")
            .containsEntry("davidia_delivery", "tok")
            .doesNotContainKey("idfa");
    }

    @Test
    void unsupportedEventIsNotSent() {
        Event impression = event(EventCodes.IMPRESSION, android());

        assertThat(new TrackProcessorKaboss(debug, SERVER).event(impression, route("ADM1"), "tok")).isEqualTo(DeliveryResult.UNSUPPORTED);
        assertThat(new TrackProcessorHaoyou(debug, SERVER).event(impression, route("https://h"), "tok")).isEqualTo(DeliveryResult.UNSUPPORTED);
    }

    @Test
    void ownProtocolAcceptsOnlyKnownEventCodes() {
        TrackProcessorDavidia davidia = new TrackProcessorDavidia(debug, SERVER);

        assertThat(davidia.callback(queries("davidia_event", EventCodes.APP_PAY, "davidia_amount", "99")).getAmount())
            .isEqualByComparingTo(new BigDecimal("99"));
        assertThat(davidia.callback(queries("davidia_event", "9901")).getEvent()).isEqualTo("9901");
        assertThat(davidia.callback(queries("davidia_event", "bogus"))).isNull();
    }

    private Map<String, String> entryParams(TrackProcessor processor, String eventCode, String trackCode, Map<String, String> queries) {
        return params(processor.buildEntryUrl(event(eventCode, queries), route(trackCode), "tok"));
    }

    private static Map<String, String> android() {
        return Map.of("oaid", "O1", "os", "Android", "ua", "Mozilla/5.0 (Linux)", "ip", "1.2.3.4", "ts", "1726650000000");
    }

    private static Map<String, String> ios() {
        return Map.of("idfa", "I1", "os", "iOS", "caid1", "c1", "caid1_v", "20250325", "ipv6", "240e::1");
    }

    private static Event event(String eventCode, Map<String, String> queries) {
        Event event = new Event();
        event.setId(7);
        event.setEvent(eventCode);
        event.setQueries(new LinkedHashMap<String, String>(queries));
        return event;
    }

    private static ClientChannelRoute route(String trackCode) {
        ClientChannelRoute route = new ClientChannelRoute();
        route.setTrackCode(trackCode);
        return route;
    }

    private static Map<String, String> queries(String... pairs) {
        Map<String, String> queries = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            queries.put(pairs[i], pairs[i + 1]);
        }
        return queries;
    }

    private static Map<String, String> params(String url) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        int query = url.indexOf('?');
        params.put("<path>", query < 0 ? url : url.substring(0, query));
        if (query >= 0) {
            for (String pair : url.substring(query + 1).split("&")) {
                int separator = pair.indexOf('=');
                params.put(pair.substring(0, separator), URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8));
            }
        }
        return params;
    }

}
