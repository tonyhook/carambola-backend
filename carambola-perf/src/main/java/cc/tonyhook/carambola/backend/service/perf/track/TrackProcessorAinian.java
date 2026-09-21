package cc.tonyhook.carambola.backend.service.perf.track;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Os;

/**
 * 爱念 oCPX 数据上报&转化回传接口文档(202606)。
 */
@Component("trackAinian")
public class TrackProcessorAinian extends TrackProcessor {

    private static final String TEMPLATE = "https://track.ainiankj.com/ocpx/media/monitor"
        + "?oid=__OID__&rt=__RT__&rid=__REQUEST_ID__&cid=__CLICK_ID__&dot=__OS__&imei_md5=__IMEI_MD5__&oaid=__OAID__&oaid_md5=__OAID_MD5__"
        + "&idfa=__IDFA__&idfa_md5=__IDFA_MD5__&caid=__CAID__&ip=__IP__&ua=__UA__&model=__MODEL__&aiCb=__CALLBACK_URL__";

    private static final Map<String, String> CONVERSIONS = Map.of(
        "1", EventCodes.APP_ACTIVATE,
        "2", EventCodes.APP_FIRST_WAKE_UP,
        "3", EventCodes.APP_REGISTER,
        "4", EventCodes.APP_RETENTION_1,
        "5", EventCodes.APP_ACTIVE,
        "6", EventCodes.APP_CHECK_OUT,
        "7", EventCodes.APP_PAY,
        "8", EventCodes.APP_PAY,
        "9", EventCodes.APP_RECALL
    );

    public TrackProcessorAinian(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("ainian", debugPrintService, appServer);
    }

    @Override
    protected boolean supports(String event) {
        return EventCodes.IMPRESSION.equals(event) || EventCodes.CLICK.equals(event);
    }

    @Override
    protected List<String> trackCodeKeys() {
        return List.of("oid");
    }

    @Override
    protected String entryUrl(Entry entry) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("__OID__", entry.code("oid"));
        // 1 曝光;2 点击
        values.put("__RT__", entry.is(EventCodes.IMPRESSION) ? "1" : "2");
        values.put("__REQUEST_ID__", entry.eventId());
        values.put("__CLICK_ID__", entry.query("click_id"));
        values.put("__CALLBACK_URL__", entry.callbackUrl());
        values.put("__IMEI_MD5__", entry.query("imei_md5"));
        values.put("__OAID__", entry.query("oaid"));
        values.put("__OAID_MD5__", entry.query("oaid_md5"));
        values.put("__IDFA__", entry.query("idfa"));
        values.put("__IDFA_MD5__", entry.query("idfa_md5"));
        values.put("__CAID__", PerfQueries.caidJson(entry.queries()));
        values.put("__IP__", entry.query("ip"));
        values.put("__UA__", entry.query("ua"));
        values.put("__MODEL__", entry.query("model"));
        // 必填:1 iOS;2 Android;3 鸿蒙
        values.put("__OS__", entry.os() == Os.IOS ? "1" : entry.os() == Os.HARMONY ? "3" : "2");
        return fillTemplate(TEMPLATE, values);
    }

    @Override
    protected String conversionEvent(Map<String, String> queries) {
        return conversion(queries, "convert_type", CONVERSIONS, null);
    }

}
