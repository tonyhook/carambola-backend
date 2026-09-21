package cc.tonyhook.carambola.backend.service.perf.media;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.EventHistoryService;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;

/**
 * YY 社区营销推广平台。回传需签名,密钥按项目分发、配在渠道上(Media.secretKey 为 ["token"])。
 */
@Component("mediaYy")
public class MediaProcessorYy extends MediaProcessor {

    private static final String CONVERSION_URL = "https://adp.yy.com/open/conversion/callback";

    // YY 的宏一律为“__参数名大写__”;不提供 os 宏,端由设备标识体现
    private static final Map<String, String> EVENT_MACROS = new LinkedHashMap<String, String>();

    static {
        EVENT_MACROS.put("ts", "__TIMESTAMP__");
        EVENT_MACROS.put("advertiser_id", "__ADVERTISER_ID__");
        EVENT_MACROS.put("project_id", "__PROJECT_ID__");
        EVENT_MACROS.put("promotion_id", "__PROMOTION_ID__");
        EVENT_MACROS.put("creative_id", "__CREATIVE_ID__");
        EVENT_MACROS.put("material_type", "__MATERIAL_TYPE__");
        EVENT_MACROS.put("location_id", "__LOCATION_ID__");
        EVENT_MACROS.put("trace_id", "__TRACE_ID__");
        EVENT_MACROS.put("rta_id", "__RTA_ID__");
        EVENT_MACROS.put("imei", "__IMEI__");
        EVENT_MACROS.put("android_id", "__ANDROID_ID__");
        EVENT_MACROS.put("oaid", "__OAID__");
        EVENT_MACROS.put("idfa", "__IDFA__");
        EVENT_MACROS.put("caid", "__CAID__");
        EVENT_MACROS.put("mac", "__MAC__");
        EVENT_MACROS.put("ip", "__IP__");
    }

    private static final Map<String, String> CONVERSIONS = Map.ofEntries(
        Map.entry(EventCodes.APP_DOWNLOAD_COMPLETED, "0"),
        Map.entry(EventCodes.PAGE_DOWNLOAD, "0"),
        Map.entry(EventCodes.APP_ACTIVATE, "1"),
        Map.entry(EventCodes.WECHAT_ACTIVATE, "1"),
        Map.entry(EventCodes.APP_REGISTER, "2"),
        Map.entry(EventCodes.PAGE_REGISTER, "2"),
        Map.entry(EventCodes.WECHAT_REGISTER, "2"),
        Map.entry(EventCodes.APP_KEY_ACTION, "3"),
        Map.entry(EventCodes.APP_CREDIT, "5"),
        Map.entry(EventCodes.PAGE_CREDIT, "5"),
        Map.entry(EventCodes.PAGE_WECHAT_COPY, "6"),
        Map.entry(EventCodes.WECHAT_FOLLOW, "6"),
        Map.entry(EventCodes.APP_ADD_TO_CART, "7"),
        Map.entry(EventCodes.PAGE_ADD_TO_CART, "7"),
        Map.entry(EventCodes.APP_PLACE_ORDER, "8"),
        Map.entry(EventCodes.PAGE_SUBMIT, "9"),
        Map.entry(EventCodes.APP_FIRST_WAKE_UP, "10"),
        Map.entry(EventCodes.APP_RETENTION_1, "10001"),
        Map.entry(EventCodes.WECHAT_RETENTION_1, "10001"),
        Map.entry(EventCodes.APP_RETENTION_3, "10002"),
        Map.entry(EventCodes.WECHAT_RETENTION_3, "10002"),
        Map.entry(EventCodes.APP_RETENTION_7, "10003"),
        Map.entry(EventCodes.WECHAT_RETENTION_7, "10003"),
        Map.entry(EventCodes.APP_PAY, "10008"),
        Map.entry(EventCodes.PAGE_PURCHASE, "10008"),
        Map.entry(EventCodes.WECHAT_PAY, "10008"),
        // 2007 是电商漏斗末端的付费,与 APP_PAY 同归付费(此前误回传成 8,即下单)
        Map.entry(EventCodes.APP_CHECK_OUT, "10008")
    );

    private static final Map<String, String> CALLBACK_TOKENS = Map.of("callback", "callback");

    // 时间窗口类事件(2021-2025)一律不映射:YY 的 eventType 全集里留存只到 10003
    // (10001 次留、10002 三日、10003 七日,没有十四日),付费只有 10008 一档不分窗口。
    //
    // 10008 的文档措辞是“当天用户在应用内完成付费”,这里的“当天”是<上报时效>——
    // 回传要与付费发生在同一天,不是“激活当天”的归因窗口。所以它对应的是不限窗口的
    // 2015 应用付费,而不是 2021 首日付费。上游回传迟到跨天时 YY 可能拒收,我们无能为力
    //
    // 付费转化要带金额与用户注册时间。按转化类型判断而不是平台事件码:
    // 同一个类型下的多个事件不会漏掉
    private static final String PAY_TYPE = "10008";

    // 付费转化要带用户注册时间,回溯同一个入口事件此前上报过的注册转化
    private static final Set<String> REGISTER_EVENTS = Set.of(
        EventCodes.APP_REGISTER,
        EventCodes.PAGE_REGISTER,
        EventCodes.WECHAT_REGISTER
    );

    private final EventHistoryService eventHistoryService;

    public MediaProcessorYy(
            PerfDebugPrintService debugPrintService,
            @Value("${app.server}") String appServer,
            EventHistoryService eventHistoryService
    ) {
        super("yy", debugPrintService, appServer);
        this.eventHistoryService = eventHistoryService;
    }

    @Override
    protected Map<String, String> eventMacros() {
        return EVENT_MACROS;
    }

    // YY 的 callback 是裸 token,不是完整地址。包成转化接口地址存进 davidia_callback,
    // 与其它媒体保持同一形态,也让它随 davidia_ 前缀一起排除在去重指纹之外
    @Override
    protected String callbackUrl() {
        return CONVERSION_URL;
    }

    @Override
    protected Map<String, String> callbackTokens() {
        return CALLBACK_TOKENS;
    }

    @Override
    protected DeliveryResult sendConversion(Event conversion, Event entry) {
        String eventType = CONVERSIONS.get(conversion.getEvent());
        if (eventType == null) {
            return DeliveryResult.UNSUPPORTED;
        }

        String secret = mediaSecret(entry, 0);
        String token = callbackParam(entry, "callback");
        if (StringUtils.isBlank(secret) || StringUtils.isBlank(token)
            || StringUtils.isBlank(conversion.getDeviceId()) || (conversion.getTime() == null)) {
            return DeliveryResult.FAILED;
        }

        // 参与签名的就是这份参数,顺序无关,签名只取值
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("callback", token);
        params.put("eventType", eventType);
        params.put("eventTime", String.valueOf(conversion.getTime().getTime()));
        params.put("userId", conversion.getDeviceId());
        if (PAY_TYPE.equals(eventType)) {
            if (conversion.getAmount() != null) {
                // payAmount 单位为分,与平台金额一致,不做换算
                params.put("payAmount", conversion.getAmount().setScale(0, RoundingMode.DOWN).toPlainString());
            }
            // 注册转化未上报过就没有这个时间,YY 可能因此拒收该条付费
            Timestamp registerTime = eventHistoryService.getFirstCallbackTime(entry, REGISTER_EVENTS);
            if (registerTime != null) {
                params.put("registerTime", String.valueOf(registerTime.getTime()));
            }
        }
        params.put("sign", sign(params, secret));

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(CONVERSION_URL);
        for (Map.Entry<String, String> param : params.entrySet()) {
            builder.queryParam(param.getKey(), param.getValue());
        }

        return get(builder, response -> response.isOkWith("code", "0"));
    }

    // 所有参数值与密钥一起升序排序,以英文逗号拼接后取 sha1
    private static String sign(Map<String, String> params, String secret) {
        List<String> values = new ArrayList<String>(params.values());
        values.add(secret);
        values.sort(null);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(String.join(",", values).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
