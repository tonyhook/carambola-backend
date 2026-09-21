package cc.tonyhook.carambola.backend.service.perf.media;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfHttp;

/**
 * 自有协议。宏表即平台词表(PerfQueries)的定义:参数名与宏名一一对应(__参数名大写__)。
 * 回传直接携带平台事件码与金额(分),不做映射。
 */
@Component("mediaDavidia")
public class MediaProcessorDavidia extends MediaProcessor {

    private static final Map<String, String> EVENT_MACROS = new LinkedHashMap<String, String>();

    static {
        EVENT_MACROS.put("davidia_callback", "__DAVIDIA_CALLBACK__");
        EVENT_MACROS.put("ts", "__TS__");
        EVENT_MACROS.put("req_id", "__REQ_ID__");
        EVENT_MACROS.put("click_id", "__CLICK_ID__");
        EVENT_MACROS.put("imei", "__IMEI__");
        EVENT_MACROS.put("imei_md5", "__IMEI_MD5__");
        EVENT_MACROS.put("android_id", "__ANDROID_ID__");
        EVENT_MACROS.put("android_id_md5", "__ANDROID_ID_MD5__");
        EVENT_MACROS.put("oaid", "__OAID__");
        EVENT_MACROS.put("oaid_md5", "__OAID_MD5__");
        EVENT_MACROS.put("idfa", "__IDFA__");
        EVENT_MACROS.put("idfa_md5", "__IDFA_MD5__");
        EVENT_MACROS.put("idfv", "__IDFV__");
        EVENT_MACROS.put("idfv_md5", "__IDFV_MD5__");
        EVENT_MACROS.put("caid1", "__CAID1__");
        EVENT_MACROS.put("caid1_md5", "__CAID1_MD5__");
        EVENT_MACROS.put("caid1_v", "__CAID1_V__");
        EVENT_MACROS.put("caid2", "__CAID2__");
        EVENT_MACROS.put("caid2_md5", "__CAID2_MD5__");
        EVENT_MACROS.put("caid2_v", "__CAID2_V__");
        EVENT_MACROS.put("aaid", "__AAID__");
        EVENT_MACROS.put("mac", "__MAC__");
        EVENT_MACROS.put("mac_md5", "__MAC_MD5__");
        EVENT_MACROS.put("ip", "__IP__");
        EVENT_MACROS.put("ipv6", "__IPV6__");
        EVENT_MACROS.put("ua", "__UA__");
        EVENT_MACROS.put("os", "__OS__");
        EVENT_MACROS.put("os_v", "__OS_V__");
    }

    public MediaProcessorDavidia(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("davidia", debugPrintService, appServer);
    }

    @Override
    protected Map<String, String> eventMacros() {
        return EVENT_MACROS;
    }

    @Override
    protected DeliveryResult sendConversion(Event conversion, Event entry) {
        UriComponentsBuilder builder = callbackBuilder(entry)
            .replaceQueryParam("davidia_event", conversion.getEvent());
        if (conversion.getAmount() != null) {
            builder.replaceQueryParam("davidia_amount", conversion.getAmount().toPlainString());
        }

        return get(builder, PerfHttp.Response::isOk);
    }

}
