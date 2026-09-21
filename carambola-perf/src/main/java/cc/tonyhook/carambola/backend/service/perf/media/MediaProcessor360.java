package cc.tonyhook.carambola.backend.service.perf.media;

import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;

/**
 * 360 点睛「搜索点击下发形式」。360 把每次点击通过 feedback_url 实时下发给监测方,
 * 那个地址就是我们的监测链接;转化经点击带回的 callback_url 回传。
 *
 * 360 另有一套「网页转化数据 API」(convert.dop.360.cn/uploadWebConvert):没有点击下发,
 * 标识由落地页带、回传是 HEADER 签名的 POST。两套参数不能混用,本类实现的是前者。
 */
@Component("media360")
public class MediaProcessor360 extends MediaProcessor {

    // 回调地址是 360 下发的模板,没被替换的宏形如 __value__
    private static final Pattern UNREPLACED_MACRO = Pattern.compile("__[A-Za-z0-9]+(?:_[A-Za-z0-9]+)*__");

    // 360 的宏一律为“__参数名__”,大小写随它自己的参数名。移动搜索才下发设备标识,
    // 不提供 os 宏,端由设备标识体现
    private static final Map<String, String> EVENT_MACROS = new LinkedHashMap<String, String>();

    static {
        EVENT_MACROS.put("click_id", "__UniqueID__");
        EVENT_MACROS.put("ts", "__clicktime__");
        EVENT_MACROS.put("ip", "__IP__");
        EVENT_MACROS.put("ua", "__UA__");
        EVENT_MACROS.put("imei_md5", "__imei_md5__");
        EVENT_MACROS.put("oaid_md5", "__oaid_md5__");
        EVENT_MACROS.put("idfa", "__IDFA__");
        EVENT_MACROS.put("advertiser_id", "__userid__");
        EVENT_MACROS.put("group_id", "__groupid__");
        EVENT_MACROS.put("plan_id", "__planid__");
        EVENT_MACROS.put("creative_id", "__creativeid__");
        EVENT_MACROS.put("keyword", "__keyword__");
        EVENT_MACROS.put("keyword_id", "__keywordid__");
        // 下发时 URLcode 编码,解出来就是完整回传地址
        EVENT_MACROS.put("davidia_callback", "__callback_url__");
    }

    // 平台事件码 → 360 转化类型。360 的表按“搜索转化名称”给,认不出的一律不回传
    private static final Map<String, String> CONVERSIONS = Map.ofEntries(
        Map.entry(EventCodes.PAGE_SUBMIT, "SUBMIT"),
        Map.entry(EventCodes.PAGE_DIAL, "CALL"),
        Map.entry(EventCodes.PAGE_DOWNLOAD, "SITEDOWNLOAD"),
        Map.entry(EventCodes.PAGE_WECHAT_COPY, "WX_BUTTON_C"),
        Map.entry(EventCodes.PAGE_REGISTER, "REGISTERED"),
        Map.entry(EventCodes.APP_REGISTER, "REGISTERED"),
        Map.entry(EventCodes.WECHAT_REGISTER, "REGISTERED"),
        Map.entry(EventCodes.PAGE_ADD_TO_CART, "ADD_TO_CART"),
        Map.entry(EventCodes.APP_ADD_TO_CART, "ADD_TO_CART"),
        Map.entry(EventCodes.PAGE_PURCHASE, "PAY"),
        Map.entry(EventCodes.APP_PAY, "PAY"),
        Map.entry(EventCodes.WECHAT_PAY, "PAY"),
        // 电商漏斗末端的付费,与 APP_PAY 同归 360 的“付费”,不是“订单提交”
        Map.entry(EventCodes.APP_CHECK_OUT, "PAY"),
        Map.entry(EventCodes.PAGE_PAYMENT_SUCCESS, "PAY_SUCCESS"),
        Map.entry(EventCodes.PAGE_CREDIT, "CREDIT"),
        Map.entry(EventCodes.APP_CREDIT, "CREDIT"),
        Map.entry(EventCodes.APP_ACTIVATE, "ACTIVATION"),
        Map.entry(EventCodes.WECHAT_ACTIVATE, "ACTIVATION"),
        Map.entry(EventCodes.APP_RETENTION_1, "RETENTION"),
        Map.entry(EventCodes.WECHAT_RETENTION_1, "RETENTION"),
        Map.entry(EventCodes.APP_PLACE_ORDER, "PLACE_ORDER"),
        // 电商漏斗起点的商品浏览
        Map.entry(EventCodes.APP_PRODUCT_VIEW, "DETAILS_PAGE_ARRIVED"),
        Map.entry(EventCodes.WECHAT_FOLLOW, "ADD_FANS_WX")
    );

    // 时间窗口类事件(2021-2025)一律不映射:360 的转化类型表里留存只有 RETENTION(次留)一档,
    // 付费只有 PAY(正式订单)、LOW_PAY(低价订单)、PAY_SUCCESS(支付成功)、APPLET_PAY(小程序内充值),
    // 没有按窗口分的类型。退回 PAY 会和 2015/2007 重复计数,所以宁可不回传
    // 只有这几类转化收金额,其余带上 value 会被当成脏数据
    private static final Set<String> AMOUNT_TYPES = Set.of("ORDER", "LOW_PAY", "PAY_SUCCESS", "PAY");

    public MediaProcessor360(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("360", debugPrintService, appServer);
    }

    @Override
    protected Map<String, String> eventMacros() {
        return EVENT_MACROS;
    }

    @Override
    protected DeliveryResult sendConversion(Event conversion, Event entry) {
        String type = CONVERSIONS.get(conversion.getEvent());
        if (type == null) {
            return DeliveryResult.UNSUPPORTED;
        }

        return get(conversionBuilder(conversion, entry, type), response -> response.isOkWith("errno", "0"));
    }

    /**
     * 回传地址由点击带回,qhclickid、qid、sendVer 等已经填好,我们只补转化本身的参数。
     * 360 下发的是模板,补不上的宏必须摘掉:把“__value__”原样发出去会被当成金额。
     */
    UriComponentsBuilder conversionBuilder(Event conversion, Event entry, String type) {
        UriComponentsBuilder builder = callbackBuilder(entry)
            .replaceQueryParam("event", type)
            .replaceQueryParam("request_time", seconds(System.currentTimeMillis()));
        // 表单与订单按 trans_id 去重,平台事件号对同一条转化恒定,重发不会被算成两条
        if (conversion.getId() != null) {
            builder.replaceQueryParam("trans_id", conversion.getId());
        }
        if (conversion.getTime() != null) {
            builder.replaceQueryParam("event_time", seconds(conversion.getTime().getTime()));
        }
        if (AMOUNT_TYPES.contains(type) && (conversion.getAmount() != null)) {
            // value 单位为分,与平台金额一致,不做换算
            builder.replaceQueryParam("value", conversion.getAmount().setScale(0, RoundingMode.DOWN).toPlainString());
        }
        dropUnreplacedMacros(builder);

        return builder;
    }

    private static void dropUnreplacedMacros(UriComponentsBuilder builder) {
        for (Map.Entry<String, List<String>> param : builder.build().getQueryParams().entrySet()) {
            for (String value : param.getValue()) {
                if ((value != null) && UNREPLACED_MACRO.matcher(value).matches()) {
                    builder.replaceQueryParam(param.getKey());
                    break;
                }
            }
        }
    }

    // 360 的时间戳一律为秒
    private static String seconds(long millis) {
        return String.valueOf(millis / 1000);
    }

}
