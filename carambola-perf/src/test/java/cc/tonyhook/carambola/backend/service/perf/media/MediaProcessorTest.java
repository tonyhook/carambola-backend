package cc.tonyhook.carambola.backend.service.perf.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfProcessors;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries;

class MediaProcessorTest {

    private static final String SERVER = "https://perf.example.com/";

    // 360 下发的回调地址:自己的字段已填好,转化相关的还是宏
    private static final String CALLBACK_360 = "https://convert.dop.360.cn/ms/third?qhclickid=abcde"
        + "&event=__event__&trans_id=__trans_id__&value=__value__&event_time=__event_time__"
        + "&request_time=__request_time__&qid=123&sendVer=10";

    private final PerfDebugPrintService debug = mock(PerfDebugPrintService.class);

    @Test
    void envelopeIsRequired() {
        MediaProcessorIfeng ifeng = new MediaProcessorIfeng(debug, SERVER);

        assertThat(ifeng.event(queries("davidia_event", EventCodes.CLICK))).isNull();
        assertThat(ifeng.event(queries("davidia_id", "MC1"))).isNull();

        Event event = ifeng.event(queries("davidia_id", "MC1", "davidia_event", EventCodes.CLICK));
        assertThat(event.getMedia()).isEqualTo("ifeng");
        assertThat(event.getMediaCode()).isEqualTo("MC1");
        assertThat(event.getEvent()).isEqualTo(EventCodes.CLICK);
    }

    @Test
    void unreplacedMediaMacrosAreDroppedAndOsNormalized() {
        MediaProcessorIfeng ifeng = new MediaProcessorIfeng(debug, SERVER);

        Event event = ifeng.event(queries("davidia_id", "MC1", "davidia_event", EventCodes.CLICK,
            "imei", "MNT_03_IMEI", "oaid", "O1", "os", "1"));

        assertThat(event.getQueries()).doesNotContainKey("imei")
            .containsEntry("oaid", "O1")
            .containsEntry("os", "iOS");
    }

    @Test
    void ownProtocolCarriesAmount() {
        MediaProcessorDavidia davidia = new MediaProcessorDavidia(debug, SERVER);

        assertThat(davidia.event(queries("davidia_id", "MC1", "davidia_event", EventCodes.APP_PAY, "davidia_amount", "1234")).getAmount())
            .isEqualByComparingTo(new BigDecimal("1234"));
        assertThat(davidia.event(queries("davidia_id", "MC1", "davidia_event", EventCodes.APP_PAY, "davidia_amount", "abc"))).isNull();
        assertThat(davidia.getEventUrl("MC1", EventCodes.CLICK))
            .startsWith("https://perf.example.com/api/open/event?davidia_media=davidia&davidia_id=MC1&davidia_event=0002&davidia_callback=__DAVIDIA_CALLBACK__&");
    }

    // 360 下发的是回传模板:该填的填上,填不上的宏要摘掉,不能原样发出去
    @Test
    void qihooFillsCallbackTemplateAndDropsUnfilledMacros() {
        MediaProcessor360 qihoo = new MediaProcessor360(debug, SERVER);
        Event entry = entry(Map.of("davidia_callback", CALLBACK_360));

        Event pay = conversion(EventCodes.APP_PAY);
        pay.setId(77);
        pay.setAmount(new BigDecimal("1234"));
        pay.setTime(new Timestamp(1726650000000L));
        Map<String, String> params = params(qihoo.conversionBuilder(pay, entry, "PAY"));

        // 点击带回的字段原样保留
        assertThat(params).containsEntry("qhclickid", "abcde")
            .containsEntry("qid", "123")
            .containsEntry("sendVer", "10")
            .containsEntry("event", "PAY")
            // value 单位为分,与平台金额一致
            .containsEntry("value", "1234")
            // 360 的时间戳是秒
            .containsEntry("event_time", "1726650000")
            .containsEntry("trans_id", "77");

        // 不收金额的转化类型不带 value,模板里那个 __value__ 一并摘掉
        Event submit = conversion(EventCodes.PAGE_SUBMIT);
        submit.setId(78);
        assertThat(params(qihoo.conversionBuilder(submit, entry, "SUBMIT")))
            .containsEntry("event", "SUBMIT")
            .doesNotContainKey("value")
            // 没有转化时间时,event_time 的宏也不能留下
            .doesNotContainKey("event_time");
    }

    // 应用内电商漏斗:2007 是漏斗末端的付费,不是下单。回传成付费才带得上金额
    @Test
    void qihooTreatsCheckOutAsPaymentNotOrder() {
        MediaProcessor360 qihoo = new MediaProcessor360(debug, SERVER);
        Event entry = entry(Map.of("davidia_callback", CALLBACK_360));

        Event checkOut = conversion(EventCodes.APP_CHECK_OUT);
        checkOut.setId(79);
        checkOut.setAmount(new BigDecimal("500"));
        Map<String, String> params = params(qihoo.conversionBuilder(checkOut, entry, "PAY"));

        assertThat(params).containsEntry("event", "PAY")
            .containsEntry("value", "500");

        // 下单是它上一步,不收金额
        Event placeOrder = conversion(EventCodes.APP_PLACE_ORDER);
        placeOrder.setId(80);
        placeOrder.setAmount(new BigDecimal("500"));
        assertThat(params(qihoo.conversionBuilder(placeOrder, entry, "PLACE_ORDER")))
            .containsEntry("event", "PLACE_ORDER")
            .doesNotContainKey("value");
    }

    @Test
    void qihooMapsOnlyKnownConversions() {
        MediaProcessor360 qihoo = new MediaProcessor360(debug, SERVER);
        Event entry = entry(Map.of("davidia_callback", CALLBACK_360));

        // 360 没有“下载完成”,只有下载按钮点击
        assertThat(qihoo.callback(conversion(EventCodes.APP_DOWNLOAD_COMPLETED), entry))
            .isEqualTo(DeliveryResult.UNSUPPORTED);
        assertThat(qihoo.callback(conversion(EventCodes.APP_KEY_ACTION), entry))
            .isEqualTo(DeliveryResult.UNSUPPORTED);
    }

    // 媒体名以数字开头:类名、bean 名、监测链接、按名字找处理器都要照常工作
    @Test
    void mediaNameMayStartWithADigit() {
        MediaProcessor360 qihoo = new MediaProcessor360(debug, SERVER);
        PerfProcessors processors = new PerfProcessors(List.of(qihoo), List.of());

        assertThat(qihoo.getName()).isEqualTo("360");
        assertThat(processors.media("360")).isSameAs(qihoo);
        assertThat(qihoo.getEventUrl("MC1", EventCodes.CLICK))
            .startsWith("https://perf.example.com/api/open/event?davidia_media=360&davidia_id=MC1&davidia_event=0002&")
            .contains("&davidia_callback=__callback_url__");

        Event event = qihoo.event(queries("davidia_id", "MC1", "davidia_event", EventCodes.CLICK,
            "davidia_callback", CALLBACK_360, "idfa", "I1", "keyword", "__keyword__"));
        assertThat(event.getMedia()).isEqualTo("360");
        // 未替换的媒体宏照常剔除
        assertThat(event.getQueries()).doesNotContainKey("keyword")
            .containsEntry("idfa", "I1");
        // 360 不下发 os,端由设备标识推断
        assertThat(PerfQueries.inferOs(event.getQueries())).isEqualTo(PerfQueries.Os.IOS);
    }

    private static Event entry(Map<String, String> queries) {
        Event entry = new Event();
        entry.setQueries(new HashMap<String, String>(queries));
        return entry;
    }

    private static Event conversion(String eventCode) {
        Event conversion = new Event();
        conversion.setEvent(eventCode);
        return conversion;
    }

    private static Map<String, String> params(UriComponentsBuilder builder) {
        Map<String, String> params = new HashMap<String, String>();
        builder.build().getQueryParams().forEach((key, values) -> params.put(key, values.get(0)));
        return params;
    }

    private static Map<String, String> queries(String... pairs) {
        Map<String, String> queries = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            queries.put(pairs[i], pairs[i + 1]);
        }
        return queries;
    }

}
