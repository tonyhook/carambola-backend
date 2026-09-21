package cc.tonyhook.carambola.backend.service.perf.track;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;

/**
 * 好友游戏。点击与回传都用我们自己的参数名与事件码。
 *
 * trackCode 是对方给的整条监测地址前缀(含 game_id/agent_id/site_id),路径里带媒体名、
 * 渠道参数逐条不同,所以整条当参数存,新开渠道不必改代码。
 *
 * 对方自行拼装回传地址,不用 davidia_callback:点击只带 davidia_track 与 davidia_delivery,
 * 他们原样存下、转化时回显。
 */
@Component("trackHaoyou")
public class TrackProcessorHaoyou extends TrackProcessor {

    // 透传的设备参数,取平台词表(PerfQueries)的参数名。取不到值的参数整个不发;
    // 对方用不上的字段自行忽略,这样接 iOS 渠道时不必再改代码
    private static final List<String> DEVICE_KEYS = List.of(
        // 安卓标识
        "imei",
        "imei_md5",
        "android_id",
        "android_id_md5",
        "oaid",
        "oaid_md5",
        // iOS 标识
        "idfa",
        "idfa_md5",
        "idfv",
        "idfv_md5",
        "caid",
        "caid1",
        "caid1_md5",
        "caid1_v",
        "caid2",
        "caid2_md5",
        "caid2_v",
        // 通用标识
        "aaid",
        "mac",
        "mac_md5",
        // 网络与设备上下文
        "ip",
        "ipv6",
        "ua",
        "os",
        "model",
        "ts"
    );

    public TrackProcessorHaoyou(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("haoyou", debugPrintService, appServer);
    }

    @Override
    protected boolean supports(String event) {
        return EventCodes.CLICK.equals(event);
    }

    @Override
    protected List<String> trackCodeKeys() {
        return List.of("url");
    }

    @Override
    protected String entryUrl(Entry entry) {
        // 没有 token 就无从归因,这条发出去也是白发
        if (StringUtils.isBlank(entry.deliveryToken())) {
            throw new IllegalStateException("delivery token is required for attribution");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(entry.code("url"));
        for (String key : DEVICE_KEYS) {
            String value = entry.query(key);
            if (value != null) {
                builder.queryParam(key, value);
            }
        }
        builder.queryParam("davidia_track", getName());
        builder.queryParam("davidia_delivery", entry.deliveryToken());
        return builder.build()
            .encode(StandardCharsets.UTF_8)
            .toUriString();
    }

    // 金额单位为分,与 Event.amount 一致
    @Override
    protected String conversionEvent(Map<String, String> queries) {
        return ownProtocolEvent(queries);
    }

    @Override
    protected String amountKey() {
        return "davidia_amount";
    }

}
