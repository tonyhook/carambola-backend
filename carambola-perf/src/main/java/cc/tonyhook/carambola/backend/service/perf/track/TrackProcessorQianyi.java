package cc.tonyhook.carambola.backend.service.perf.track;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfHttp;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Os;

/**
 * 千易 CPA 监测接入文档 V1.7。
 */
@Component("trackQianyi")
public class TrackProcessorQianyi extends TrackProcessor {

    private static final String TEMPLATE = "https://ad.qianyichuanmei.com.cn/cpa/ocpx/__action__"
        + "?fid=__fid__&rid=__request_id__&cb=__callback__&ua=__user_agent__&tms=__tms__&ip=__ip__&ipv6=__ipv6__"
        + "&imei=__imei__&imeimd5=__imei_md5__&oid=__oaid__&oidmd5=__oaid_md5__&aid=__android_id__&aidmd5=__android_id_md5__"
        + "&idfa=__idfa__&idfamd5=__idfa_md5__&caid1=__caid1__&caidVer1=__caid_ver1__&caid2=__caid2__&caidVer2=__caid_ver2__&caid=__caid__"
        + "&mac=__mac__&macmd5=__mac_md5__&brand=__brand__&model=__model__&ost=__os_type__&aaid=__aaid__";

    private static final Map<String, String> CONVERSIONS = Map.of(
        "1", EventCodes.APP_ACTIVATE,
        "2", EventCodes.APP_REGISTER,
        "3", EventCodes.APP_RETENTION_1,
        "4", EventCodes.APP_PAY,
        "5", EventCodes.APP_LATER_WAKE_UP,
        "6", EventCodes.APP_FIRST_WAKE_UP,
        "7", EventCodes.APP_RETENTION_3,
        "8", EventCodes.APP_RETENTION_7
    );

    public TrackProcessorQianyi(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("qianyi", debugPrintService, appServer);
    }

    @Override
    protected boolean supports(String event) {
        return EventCodes.IMPRESSION.equals(event) || EventCodes.CLICK.equals(event);
    }

    @Override
    protected List<String> trackCodeKeys() {
        return List.of("fid");
    }

    @Override
    protected String entryUrl(Entry entry) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("__action__", entry.is(EventCodes.IMPRESSION) ? "imp" : "click");
        values.put("__fid__", entry.code("fid"));
        values.put("__request_id__", entry.eventId());
        values.put("__callback__", entry.callbackUrl());
        values.put("__tms__", entry.query("ts"));
        values.put("__imei__", entry.query("imei"));
        values.put("__imei_md5__", entry.query("imei_md5"));
        values.put("__oaid__", entry.query("oaid"));
        values.put("__oaid_md5__", entry.query("oaid_md5"));
        values.put("__android_id__", entry.query("android_id"));
        values.put("__android_id_md5__", entry.query("android_id_md5"));
        values.put("__idfa__", entry.query("idfa"));
        values.put("__idfa_md5__", entry.query("idfa_md5"));
        values.put("__caid1__", entry.query("caid1"));
        values.put("__caid_ver1__", entry.query("caid1_v"));
        values.put("__caid2__", entry.query("caid2"));
        values.put("__caid_ver2__", entry.query("caid2_v"));
        values.put("__caid__", PerfQueries.caidVersioned(entry.queries()));
        values.put("__aaid__", entry.query("aaid"));
        values.put("__mac__", entry.query("mac"));
        values.put("__mac_md5__", entry.query("mac_md5"));
        values.put("__ip__", entry.query("ip"));
        values.put("__ipv6__", entry.query("ipv6"));
        values.put("__user_agent__", entry.query("ua"));
        values.put("__brand__", entry.query("brand"));
        values.put("__model__", entry.query("model"));
        // 0=Unknown;1=IOS;2=Android
        values.put("__os_type__", entry.os() == Os.IOS ? "1" : entry.os() == Os.ANDROID ? "2" : "0");
        return fillTemplate(TEMPLATE, values);
    }

    @Override
    protected boolean accepted(PerfHttp.Response response) {
        return response.isOkWith("code", "0");
    }

    @Override
    protected String conversionEvent(Map<String, String> queries) {
        return conversion(queries, "transformType", CONVERSIONS, null);
    }

}
