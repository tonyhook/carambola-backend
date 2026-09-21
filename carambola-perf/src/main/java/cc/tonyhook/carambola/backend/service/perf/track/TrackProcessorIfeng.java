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
 * 凤凰网监测接入文档(平台侧)。上报域名由对方在投放时提供,和 cid 等固定参数一起配在 trackCode 上。
 */
@Component("trackIfeng")
public class TrackProcessorIfeng extends TrackProcessor {

    private static final String TEMPLATE = "/d/__ACTION__"
        + "?cid=__CID__&gid=__GID__&aid=__AID__&mid=__MID__&channel=__CHANNEL__&reqid=__REQ_ID__&tagid=__TAGID__&os=__OS__&ip=__IP__&ua=__UA__"
        + "&model=__MODEL__&ts=__TS__&idfa=__IDFA__&idfa_md5=__IDFA_MD5__&caid=__CAID__&imei=__IMEI__&oaid=__OAID__&oaid_md5=__OAID_MD5__"
        + "&android_id=__ANDROID_ID__&callback=__CALLBACK_URL__";

    private static final Map<String, String> CONVERSIONS = Map.of(
        "download", EventCodes.APP_DOWNLOAD_COMPLETED,
        "active", EventCodes.APP_ACTIVATE,
        "activity", EventCodes.APP_ACTIVE,
        "recall", EventCodes.APP_RECALL,
        "register", EventCodes.APP_REGISTER,
        "pay", EventCodes.APP_PAY,
        "addiction", EventCodes.APP_KEY_ACTION,
        "next_day", EventCodes.APP_RETENTION_1,
        "first_order", EventCodes.APP_PLACE_ORDER
    );

    public TrackProcessorIfeng(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("ifeng", debugPrintService, appServer);
    }

    @Override
    protected boolean supports(String event) {
        return EventCodes.IMPRESSION.equals(event) || EventCodes.CLICK.equals(event);
    }

    @Override
    protected List<String> trackCodeKeys() {
        return List.of("cid", "gid", "aid", "mid", "channel", "host");
    }

    @Override
    protected String entryUrl(Entry entry) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("__ACTION__", entry.is(EventCodes.IMPRESSION) ? "imp" : "cli");
        values.put("__CID__", entry.code("cid"));
        values.put("__GID__", entry.code("gid"));
        values.put("__AID__", entry.code("aid"));
        values.put("__MID__", entry.code("mid"));
        values.put("__CHANNEL__", entry.code("channel"));
        values.put("__REQ_ID__", entry.eventId());
        values.put("__CALLBACK_URL__", entry.callbackUrl());
        values.put("__TS__", entry.query("ts"));
        values.put("__TAGID__", entry.query("tagid"));
        values.put("__IP__", entry.query("ip"));
        values.put("__UA__", entry.query("ua"));
        values.put("__MODEL__", entry.query("model"));
        values.put("__IDFA__", entry.query("idfa"));
        values.put("__IDFA_MD5__", entry.query("idfa_md5"));
        values.put("__IMEI__", entry.query("imei"));
        values.put("__OAID__", entry.query("oaid"));
        values.put("__OAID_MD5__", entry.query("oaid_md5"));
        values.put("__ANDROID_ID__", entry.query("android_id"));
        values.put("__CAID__", PerfQueries.caidVersioned(entry.queries()));
        // 0 安卓;1 iOS
        values.put("__OS__", entry.os() == Os.ANDROID ? "0" : entry.os() == Os.IOS ? "1" : null);
        return trimTrailingSlash(entry.code("host")) + fillTemplate(TEMPLATE, values);
    }

    @Override
    protected boolean accepted(PerfHttp.Response response) {
        return response.isOkWith("code", "0");
    }

    // 协议规定:无 event_type 字段默认为激活
    @Override
    protected String conversionEvent(Map<String, String> queries) {
        return conversion(queries, "event_type", CONVERSIONS, EventCodes.APP_ACTIVATE);
    }

    @Override
    protected String amountKey() {
        return "pay_amount";
    }

}
