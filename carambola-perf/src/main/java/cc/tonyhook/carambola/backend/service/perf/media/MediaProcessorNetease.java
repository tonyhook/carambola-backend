package cc.tonyhook.carambola.backend.service.perf.media;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Caid;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Os;

/**
 * 有道。回传地址由入口事件带回的转化标识拼出,转化事件名按端加前缀(android_/ios_)。
 */
@Component("mediaNetease")
public class MediaProcessorNetease extends MediaProcessor {

    private static final String CONVERSION_URL = "http://conv.youdao.com/api/track";

    private static final Map<String, String> EVENT_MACROS = new LinkedHashMap<String, String>();

    static {
        EVENT_MACROS.put("conv", "__conv__");
        EVENT_MACROS.put("device_id", "__device_id__");
        EVENT_MACROS.put("imei_md5", "__imei__");
        EVENT_MACROS.put("android_id_md5", "__android_id__");
        EVENT_MACROS.put("oaid", "__oaid__");
        EVENT_MACROS.put("oaid_md5", "__oaid_md5__");
        EVENT_MACROS.put("idfa", "__idfa__");
        EVENT_MACROS.put("idfa_md5", "__idfa_md5__");
        EVENT_MACROS.put("caid_list", "__caid__");
        EVENT_MACROS.put("caid_md5_list", "__caid_md5__");
        EVENT_MACROS.put("aaid", "__aaid__");
        EVENT_MACROS.put("req_id", "__req_id__");
        EVENT_MACROS.put("ip", "__ip__");
        EVENT_MACROS.put("ipv6", "__ipv6__");
        EVENT_MACROS.put("ua", "__ua__");
        EVENT_MACROS.put("mac_md5", "__mac__");
        // 取值为 ANDROID/IOS
        EVENT_MACROS.put("os", "__os__");
        EVENT_MACROS.put("model", "__model__");
        EVENT_MACROS.put("ts", "__ts__");
    }

    // 有道转化跟踪专用字段 → 回传参数名:应用下载与落地页回传 conv_ext,微信小程序回传 callback,
    // 直达链接回传 req_id。小程序与直达链接的标识由调起的小程序或应用带回,不经过入口事件监测链接
    private static final Map<String, String> CALLBACK_TOKENS = new LinkedHashMap<String, String>();

    static {
        CALLBACK_TOKENS.put("conv", "conv_ext");
        CALLBACK_TOKENS.put("callback", "callback");
        CALLBACK_TOKENS.put("req_id", "req_id");
    }

    // 平台事件码 → 转化事件名(不含端前缀)。
    // 这里实现的是有道的「应用下载 API」,事件名一律带 android_/ios_ 前缀,所以只收 APP_ 侧的事件码;
    // 页面类(1008 加购、1013 授信等)属于有道另一套「落地页 API」,事件名不同,不能混进来
    private static final Map<String, String> CONVERSIONS = Map.ofEntries(
        Map.entry(EventCodes.APP_DOWNLOAD_COMPLETED, "download"),
        Map.entry(EventCodes.APP_ACTIVATE, "activate"),
        Map.entry(EventCodes.APP_REGISTER, "register"),
        Map.entry(EventCodes.APP_RETENTION_1, "day1retention"),
        Map.entry(EventCodes.APP_RETENTION_3, "retention"),
        Map.entry(EventCodes.APP_RETENTION_7, "retention"),
        Map.entry(EventCodes.APP_RETENTION_14, "retention"),
        Map.entry(EventCodes.APP_FIRST_WAKE_UP, "in_app_uv"),
        Map.entry(EventCodes.APP_ADD_TO_CART, "addtocart"),
        Map.entry(EventCodes.APP_PLACE_ORDER, "in_app_order"),
        Map.entry(EventCodes.APP_CREDIT, "credit"),
        // 2007 是电商漏斗末端的付费,与 APP_PAY 同归 purchase(此前误回传成 in_app_order)
        Map.entry(EventCodes.APP_CHECK_OUT, "purchase"),
        Map.entry(EventCodes.APP_PAY, "purchase")
    );

    // 订单类转化,带 order_id 与 order_amount。按转化类型判断而不是平台事件码:
    // 同一个类型下的多个事件不会漏掉。下单与购买都受“没有 order_id 只收第一笔”的限制
    private static final Set<String> ORDER_ACTIONS = Set.of("purchase", "in_app_order");

    // 留存天数反过来只能按平台事件码分:三日与七日回传的是同一个 action
    // 分窗口付费(2021-2024)不映射:有道的转化事件只有 purchase / in_app_order / addtocart /
    // credit / in_app_uv / custom 等,没有按时间窗口分的付费,退回 purchase 会和 2015/2007 重复计数
    //
    // 留存天数反过来只能按平台事件码分:三日以上的留存回传的是同一个 action,靠 retention_days 区分。
    // 文档给的取值范围是 [2,30],所以十四日留存直接复用 retention
    private static final Map<String, String> RETENTION_DAYS = Map.of(
        EventCodes.APP_RETENTION_3, "3",
        EventCodes.APP_RETENTION_7, "7",
        EventCodes.APP_RETENTION_14, "14"
    );

    public MediaProcessorNetease(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("netease", debugPrintService, appServer);
    }

    @Override
    protected Map<String, String> eventMacros() {
        return EVENT_MACROS;
    }

    // 有道的监测地址只支持 80 端口(http 默认端口),这里把 app.server 降级为 http。
    // 需要 nginx 在 80 端口上直连 /api/open/ 而不是跳转到 https,否则有道取不到入口事件
    @Override
    protected String eventServer() {
        return super.eventServer().replaceFirst("^https://", "http://");
    }

    // 有道的 __caid__ 与 __caid_md5__ 是两个独立的选填宏,两边的条数和版本号集合未必一致,
    // 明文与 md5 各自解析后交给 PerfQueries 按版本号归并
    @Override
    protected void collectCaid(Map<String, String> queries, Caid caid) {
        collect(queries.get("caid_list"), caid::add);
        collect(queries.get("caid_md5_list"), caid::addMd5);
    }

    @Override
    protected String callbackUrl() {
        return CONVERSION_URL;
    }

    // 回传字段名取决于入口事件带回的是哪个标识,判据只存在于入站时刻,回传地址在那时就定好
    @Override
    protected Map<String, String> callbackTokens() {
        return CALLBACK_TOKENS;
    }

    @Override
    protected DeliveryResult sendConversion(Event conversion, Event entry) {
        String action = CONVERSIONS.get(conversion.getEvent());
        if (action == null) {
            return DeliveryResult.UNSUPPORTED;
        }
        // __os__ 是选填宏,缺失时按入口事件带回的设备标识推断
        Os os = PerfQueries.inferOs(entry.getQueries());
        if (os == null) {
            return DeliveryResult.FAILED;
        }
        // 有道只有 android_download,iOS 无对应事件
        if ("download".equals(action) && (os != Os.ANDROID)) {
            return DeliveryResult.UNSUPPORTED;
        }

        UriComponentsBuilder builder = callbackBuilder(entry)
            .replaceQueryParam("conv_action", (os == Os.IOS ? "ios_" : "android_") + action);
        if (conversion.getTime() != null) {
            // conv_time 单位为毫秒。不给的话有道按收到回传的时刻记,归因时间会偏
            builder.replaceQueryParam("conv_time", conversion.getTime().getTime());
        }
        if (ORDER_ACTIONS.contains(action)) {
            // 同一次入口事件下的多笔购买靠 order_id 区分,不给有道只收第一笔。
            // 平台事件号对同一条转化恒定,重发不会被当成新的一笔
            if (conversion.getId() != null) {
                builder.replaceQueryParam("order_id", conversion.getId());
            }
            if (conversion.getAmount() != null) {
                // order_amount 单位为分,与平台金额一致,不做换算
                builder.replaceQueryParam("order_amount", conversion.getAmount().setScale(0, RoundingMode.DOWN).toPlainString());
            }
        }
        if (RETENTION_DAYS.containsKey(conversion.getEvent())) {
            builder.replaceQueryParam("retention_days", RETENTION_DAYS.get(conversion.getEvent()));
        }

        return get(builder, response -> response.isOkWith("process_code", "0"));
    }

    // 形如“CAID_版本号”,多个以英文逗号拼接。版本号为空的(未被替换的宏)由 Caid 挡掉
    private static void collect(String source, BiConsumer<String, String> sink) {
        if (source == null) {
            return;
        }

        for (String item : source.split(",")) {
            int separator = item.lastIndexOf("_");
            if (separator < 0) {
                continue;
            }
            sink.accept(item.substring(separator + 1), item.substring(0, separator));
        }
    }

}
