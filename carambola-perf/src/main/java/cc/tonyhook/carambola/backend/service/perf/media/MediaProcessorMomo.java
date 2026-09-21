package cc.tonyhook.carambola.backend.service.perf.media;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;

/**
 * 陌陌。回传不走入口事件带来的地址,而是 POST 到固定的转化接口,
 * 入口事件带来的 CALLBACK 只用来取加密字段 encrypt。
 */
@Component("mediaMomo")
public class MediaProcessorMomo extends MediaProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CONVERSION_URL = "https://api-vip.immomo.com/ad/api/conversion";

    // 陌陌签名的排除字段,与文档给出的 EXCLUDE 一致
    private static final Set<String> SIGN_EXCLUDED = Set.of(
        "sign",
        "encrypt",
        "extraInfo",
        "convImei",
        "convIdfa",
        "convOaid",
        "clientIp",
        "model",
        "ua",
        "mac"
    );

    private static final Map<String, String> EVENT_MACROS = new LinkedHashMap<String, String>();

    static {
        EVENT_MACROS.put("davidia_callback", "[CALLBACK]");
        EVENT_MACROS.put("trace_id", "[TRACE_ID]");
        EVENT_MACROS.put("customer_id", "[CUSTOMER_ID]");
        EVENT_MACROS.put("campaign_id", "[CAMPAIGN_ID]");
        EVENT_MACROS.put("ad_id", "[AD_ID]");
        EVENT_MACROS.put("creative_id", "[CREATIVE_ID]");
        EVENT_MACROS.put("imei_md5", "[IMEI]");
        EVENT_MACROS.put("oaid", "[OAID]");
        EVENT_MACROS.put("android_id_md5", "[ANDROIDID]");
        EVENT_MACROS.put("idfa", "[IDFA]");
        EVENT_MACROS.put("caid1", "[CAID1]");
        EVENT_MACROS.put("caid1_v", "[CAID1_VERSION]");
        EVENT_MACROS.put("caid2", "[CAID2]");
        EVENT_MACROS.put("caid2_v", "[CAID2_VERSION]");
        EVENT_MACROS.put("ip", "[IP]");
        EVENT_MACROS.put("ua", "[UA]");
        // 取值为 Android、IOS、OTHERs
        EVENT_MACROS.put("os", "[OS]");
    }

    private static final Map<String, Integer> CONVERSIONS = Map.ofEntries(
        Map.entry(EventCodes.APP_ACTIVATE, 0),
        Map.entry(EventCodes.WECHAT_ACTIVATE, 0),
        Map.entry(EventCodes.APP_REGISTER, 1),
        Map.entry(EventCodes.PAGE_REGISTER, 1),
        Map.entry(EventCodes.WECHAT_REGISTER, 1),
        Map.entry(EventCodes.APP_PAY, 2),
        Map.entry(EventCodes.PAGE_PURCHASE, 2),
        Map.entry(EventCodes.WECHAT_PAY, 2),
        // 2007 是电商漏斗末端的付费,和 APP_PAY 同归陌陌的付费
        Map.entry(EventCodes.APP_CHECK_OUT, 2),
        Map.entry(EventCodes.PAGE_SUBMIT, 4),
        Map.entry(EventCodes.PAGE_WECHAT_COPY, 5),
        Map.entry(EventCodes.APP_KEY_ACTION, 6)
    );

    // 留存类(2004/2018/2005/2025)与分窗口付费(2021-2024)一律不映射:陌陌的 type 全集就是
    // 0=激活、1=注册、2=付费、4=表单提交、5=添加企业微信、6=关键行为、7=创建角色,
    // 既没有留存,付费也只有不分窗口的一档
    // 渠道上没配密钥时的全局兜底,待各渠道都按 Media.secretKey 配好后移除
    private final String fallbackSecretKey;

    public MediaProcessorMomo(
            PerfDebugPrintService debugPrintService,
            @Value("${app.server}") String appServer,
            @Value("${app.momo-secret-key:}") String fallbackSecretKey
    ) {
        super("momo", debugPrintService, appServer);
        this.fallbackSecretKey = fallbackSecretKey;
    }

    @Override
    protected Map<String, String> eventMacros() {
        return EVENT_MACROS;
    }

    @Override
    protected DeliveryResult sendConversion(Event conversion, Event entry) {
        Integer type = CONVERSIONS.get(conversion.getEvent());
        if (type == null) {
            return DeliveryResult.UNSUPPORTED;
        }

        // 陌陌的加密字段由原始 CALLBACK 地址带来,原样回传
        String encrypt = callbackParam(entry, "encrypt");
        if ((encrypt == null) || (query(entry, "trace_id") == null) || (conversion.getTime() == null)) {
            return DeliveryResult.FAILED;
        }

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("type", type);
        body.put("convTime", conversion.getTime().getTime());
        body.put("encrypt", encrypt);
        body.put("traceId", query(entry, "trace_id"));
        if (conversion.getAmount() != null) {
            // convValue 为整数,按平台金额(分)向下取整
            body.put("convValue", conversion.getAmount().setScale(0, RoundingMode.DOWN).longValue());
        }
        // 以下字段不参与签名,按陌陌的字段名回传入站时采集到的值
        putIfPresent(body, "convImei", query(entry, "imei_md5"));
        putIfPresent(body, "convIdfa", query(entry, "idfa"));
        putIfPresent(body, "convOaid", query(entry, "oaid"));
        putIfPresent(body, "clientIp", query(entry, "ip"));
        putIfPresent(body, "ua", query(entry, "ua"));

        String secret = StringUtils.defaultIfBlank(mediaSecret(entry, 0), fallbackSecretKey);
        String sign = sign(body, secret);
        if (sign != null) {
            body.put("sign", sign);
        }

        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return DeliveryResult.FAILED;
        }
        return postJson(CONVERSION_URL, json, response -> {
            Map<String, Object> result = response.json();
            return (result != null) && "0".equals(String.valueOf(result.get("ec"))) && Boolean.TRUE.equals(result.get("data"));
        });
    }

    private static void putIfPresent(Map<String, Object> body, String key, String value) {
        if (value != null) {
            body.put(key, value);
        }
    }

    // 排除字段之外的参数按键名升序以 & 拼接,HmacSHA256 后做无填充的 URL 安全 Base64
    private static String sign(Map<String, Object> body, String secret) {
        if (StringUtils.isBlank(secret)) {
            return null;
        }

        Map<String, String> signParams = new TreeMap<String, String>();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            if ((entry.getValue() != null) && !SIGN_EXCLUDED.contains(entry.getKey())) {
                signParams.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, String> entry : signParams.entrySet()) {
            if (content.length() > 0) {
                content.append("&");
            }
            content.append(entry.getKey()).append("=").append(entry.getValue());
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(content.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

}
