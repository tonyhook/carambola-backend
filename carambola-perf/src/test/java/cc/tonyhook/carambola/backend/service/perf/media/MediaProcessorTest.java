package cc.tonyhook.carambola.backend.service.perf.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfProcessors;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries;

class MediaProcessorTest {

    private static final String SERVER = "https://perf.example.com/";

    // 本机的 discard 端口:连接立即被拒,回传不会真的发出去
    private static final String DEAD_CALLBACK = "http://127.0.0.1:9/cb";

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
    void neteaseMergesCaidByVersionAndBuildsCallbackFromConversionKey() {
        MediaProcessorNetease netease = new MediaProcessorNetease(debug, SERVER);

        Event event = netease.event(queries("davidia_id", "MC1", "davidia_event", EventCodes.CLICK,
            "caid_list", "a_20230330,b_20250325", "caid_md5_list", "bmd5_20250325",
            "conv", "CONV1", "req_id", "R1"));

        assertThat(event.getQueries()).containsEntry("caid1", "b")
            .containsEntry("caid1_md5", "bmd5")
            .containsEntry("caid2", "a")
            .doesNotContainKey("caid2_md5")
            // conv 优先于 req_id,源字段移除
            .containsEntry("davidia_callback", "http://conv.youdao.com/api/track?conv_ext=CONV1")
            .doesNotContainKey("conv")
            .containsEntry("req_id", "R1");
        assertThat(netease.getEventUrl("MC1", EventCodes.CLICK)).startsWith("http://perf.example.com/api/open/event?davidia_media=netease&");
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

    // 有道的多笔购买靠 order_id 区分,不给它只收第一笔;conv_time 不给则按收到时刻记
    @Test
    void neteaseSendsOrderIdAndConversionTimeForPurchases() {
        MediaProcessorNetease netease = new MediaProcessorNetease(debug, SERVER);

        assertThat(sent(netease, EventCodes.APP_PAY, DEAD_CALLBACK))
            .contains("conv_action=android_purchase")
            .contains("order_id=1")
            .contains("order_amount=1234")
            // 有道的 conv_time 是毫秒,不是秒
            .contains("conv_time=1726650000000");

        // 非购买类转化不带订单参数,但时间照常带
        assertThat(sent(netease, EventCodes.APP_ACTIVATE, DEAD_CALLBACK))
            .contains("conv_action=android_activate")
            .contains("conv_time=1726650000000")
            .doesNotContain("order_id")
            .doesNotContain("order_amount");
    }

    // 有道有 addtocart 与 credit,我们早有 2006/2011,此前一直没连上
    @Test
    void neteaseSendsAddToCartAndCredit() {
        MediaProcessorNetease netease = new MediaProcessorNetease(debug, SERVER);
        Event entry = entry(Map.of("davidia_callback", DEAD_CALLBACK, "idfa", "I1"));

        // idfa 推断出 iOS,端前缀随之变化
        assertThat(sent(netease, EventCodes.APP_ADD_TO_CART, DEAD_CALLBACK))
            .contains("conv_action=android_addtocart")
            .doesNotContain("order_amount");
        assertThat(netease.callback(payConversion(EventCodes.APP_CREDIT), entry))
            .isNotEqualTo(DeliveryResult.UNSUPPORTED);

        // 页面侧属于有道另一套落地页 API,不能混进应用下载 API
        assertThat(netease.callback(conversion(EventCodes.PAGE_ADD_TO_CART), entry))
            .isEqualTo(DeliveryResult.UNSUPPORTED);
        assertThat(netease.callback(conversion(EventCodes.PAGE_CREDIT), entry))
            .isEqualTo(DeliveryResult.UNSUPPORTED);
    }

    // 有道的留存靠 retention_days 区分,文档取值范围 [2,30],所以十四日留存复用 retention
    @Test
    void neteaseSendsFourteenDayRetentionAsRetentionDays() {
        MediaProcessorNetease netease = new MediaProcessorNetease(debug, SERVER);

        assertThat(sent(netease, EventCodes.APP_RETENTION_14, DEAD_CALLBACK))
            .contains("conv_action=android_retention")
            .contains("retention_days=14");
        // 次留是独立的转化事件,不带 retention_days
        assertThat(sent(netease, EventCodes.APP_RETENTION_1, DEAD_CALLBACK))
            .contains("conv_action=android_day1retention")
            .doesNotContain("retention_days");
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

    // 回传前处理器会把地址打进 debugPrintService,据此读回实际发出的 URL。
    // 地址指向本机关闭的端口,请求立刻被拒,不会真的联网
    private String sent(MediaProcessor media, String eventCode, String callback) {
        Event conversion = conversion(eventCode);
        conversion.setId(1);
        conversion.setAmount(new BigDecimal("1234"));
        conversion.setTime(new Timestamp(1726650000000L));
        // oaid 让有道推断出 Android
        media.callback(conversion, entry(Map.of(
            "davidia_callback", callback, "oaid", "O1", "trace_id", "T1")));

        ArgumentCaptor<String> printed = ArgumentCaptor.forClass(String.class);
        verify(debug, atLeastOnce()).println(printed.capture());
        return printed.getAllValues().stream()
            .filter(line -> line.startsWith("callbackB:"))
            .reduce((first, last) -> last)
            .orElseThrow();
    }

    private static Event payConversion(String eventCode) {
        Event conversion = conversion(eventCode);
        conversion.setId(1);
        conversion.setAmount(new BigDecimal("1234"));
        conversion.setTime(new Timestamp(1726650000000L));
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
