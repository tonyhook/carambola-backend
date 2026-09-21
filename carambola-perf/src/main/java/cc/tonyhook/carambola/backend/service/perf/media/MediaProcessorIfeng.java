package cc.tonyhook.carambola.backend.service.perf.media;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfHttp;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Os;

/**
 * 凤羽 OCPC 对接文档(应用下载 API 部分)。
 *
 * 回传地址对监测方不透明,转化目标由对方在创建转化时选定、编码在地址里(convertId),
 * 回传请求本身不带事件类型。因此这里不做事件映射,凡是路由到这里的转化都原样回调。
 */
@Component("mediaIfeng")
public class MediaProcessorIfeng extends MediaProcessor {

    private static final Map<String, String> EVENT_MACROS = new LinkedHashMap<String, String>();

    static {
        EVENT_MACROS.put("ts", "MNT_10_TS");
        EVENT_MACROS.put("req_id", "MNT_09_REQ");
        EVENT_MACROS.put("imei", "MNT_03_IMEI");
        EVENT_MACROS.put("imei_md5", "MNT_03_MD5_IMEI");
        EVENT_MACROS.put("oaid", "MNT_14_OAID");
        EVENT_MACROS.put("idfa", "MNT_04_IDFA");
        EVENT_MACROS.put("idfa_md5", "MNT_04_MD5_IDFA");
        EVENT_MACROS.put("mac_md5", "MNT_02_MAC");
        EVENT_MACROS.put("ip", "MNT_01_IP");
        EVENT_MACROS.put("ua", "MNT_18_UA");
        EVENT_MACROS.put("os", "MNT_00_OS");
        // 协议要求回调地址放在最后,其后的宏不会被替换
        EVENT_MACROS.put("davidia_callback", "MNT_08_CALLBACK");
    }

    // android:0, ios:1, windows nt:2, mac:3
    private static final Map<String, Os> OS_ALIASES = Map.of(
        "0", Os.ANDROID,
        "1", Os.IOS,
        "2", Os.WINDOWS,
        "3", Os.MACOS
    );

    public MediaProcessorIfeng(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("ifeng", debugPrintService, appServer);
    }

    @Override
    protected Map<String, String> eventMacros() {
        return EVENT_MACROS;
    }

    @Override
    protected Map<String, Os> osAliases() {
        return OS_ALIASES;
    }

    @Override
    protected DeliveryResult sendConversion(Event conversion, Event entry) {
        // 成功返回 200,失败返回其他的 http 状态码
        return get(callbackBuilder(entry), PerfHttp.Response::isOk);
    }

}
