package cc.tonyhook.carambola.backend.service.perf.media;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Caid;

/**
 * 掌阅。
 */
@Component("mediaZhangyue")
public class MediaProcessorZhangyue extends MediaProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<String, String> EVENT_MACROS = new LinkedHashMap<String, String>();

    static {
        EVENT_MACROS.put("ts", "_TS_");
        EVENT_MACROS.put("click_id", "_CID_");
        EVENT_MACROS.put("imei_md5", "_IMEI_MD5_");
        EVENT_MACROS.put("oaid", "_OAID_");
        EVENT_MACROS.put("oaid_md5", "_OAIDMD5_");
        EVENT_MACROS.put("android_id", "_ANDROIDID_");
        EVENT_MACROS.put("idfa", "_IDFA_");
        EVENT_MACROS.put("caid", "_CAID_");
        EVENT_MACROS.put("caid2", "_CAID2_");
        EVENT_MACROS.put("caid_list", "_CAIDV_");
        EVENT_MACROS.put("ip", "_IP_");
        EVENT_MACROS.put("ua", "_UA_");
        EVENT_MACROS.put("os", "_OS_");
        EVENT_MACROS.put("davidia_callback", "_CALLBACK_URL_");
    }

    private static final Map<String, String> CONVERSIONS = Map.of(
        EventCodes.APP_ACTIVATE, "act",
        EventCodes.APP_REGISTER, "register",
        EventCodes.APP_RETENTION_1, "leave",
        EventCodes.APP_FIRST_WAKE_UP, "active",
        EventCodes.APP_PLACE_ORDER, "submitorder",
        // 2007 是电商漏斗末端的付费,与 APP_PAY 同归 pay(此前误回传成 submitorder)。
        // 不要因为名字像就改成 ecommerce_pay:掌阅的 ecommerce_pay 是“由外部电商平台回传的付费”,
        // 而 pay 才是“用户激活后,在应用内完成了付费”——2007 是应用内电商漏斗,归 pay
        EventCodes.APP_CHECK_OUT, "pay",
        EventCodes.APP_PAY, "pay"
    );

    // 时间窗口类事件(2021-2025)一律不映射:掌阅的留存只有 leave(次留)一档,没有三日/七日/十四日;
    // 付费虽然有 pay / game_pay / ecommerce_pay / live_pay / playlet_pay 多个,但分的是场景不是时间窗口
    //
    // 收金额的转化类型。按转化类型判断而不是平台事件码:同一个类型下的多个事件不会漏掉
    private static final Set<String> AMOUNT_TYPES = Set.of("pay");

    public MediaProcessorZhangyue(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("zhangyue", debugPrintService, appServer);
    }

    @Override
    protected Map<String, String> eventMacros() {
        return EVENT_MACROS;
    }

    // 掌阅的 _CAIDV_ 是 [{"caid":…,"version":…},…],只有明文没有 md5
    @Override
    protected void collectCaid(Map<String, String> queries, Caid caid) {
        String source = queries.get("caid_list");
        if (source == null) {
            return;
        }

        try {
            List<Map<String, Object>> caidList = OBJECT_MAPPER.readValue(
                source,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            for (Map<String, Object> item : caidList) {
                caid.add(text(item.get("version")), text(item.get("caid")));
            }
        } catch (Exception e) {
            // 解析不了就只保留原串
        }
    }

    @Override
    protected DeliveryResult sendConversion(Event conversion, Event entry) {
        String type = CONVERSIONS.get(conversion.getEvent());
        if (type == null) {
            return DeliveryResult.UNSUPPORTED;
        }

        UriComponentsBuilder builder = callbackBuilder(entry)
            .replaceQueryParam("type", type);
        if (AMOUNT_TYPES.contains(type) && (conversion.getAmount() != null)) {
            builder.replaceQueryParam("pay_amount", formatPayAmount(conversion.getAmount()));
        }

        return get(builder, response -> response.isOkWith("type", "success"));
    }

    // 掌阅的 pay_amount 单位为元
    private String formatPayAmount(BigDecimal amount) {
        return amount.divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN).toPlainString();
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
