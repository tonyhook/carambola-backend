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
 * 卡宝 渠道OCPX对接文档 1.0.4。
 */
@Component("trackKaboss")
public class TrackProcessorKaboss extends TrackProcessor {

    private static final String TEMPLATE = "https://ad.kaboss.cn/media/monitor/commonClick/distributor"
        + "?imei=__IMEI__&imei_md5=__IMEI_MD5__&oaid=__OAID__&oaid_md5=__OAID_MD5__&android_id=__ANDROID_ID__&android_id_md5=__ANDROID_ID_MD5__"
        + "&callback_url=__CALLBACK_URL__&project_id=__PROJECT_ID__&plan_id=__PLAN_ID__&plan_name=__PLAN_NAME__&unit_id=__UNIT_ID__&unit_name=__UNIT_NAME__"
        + "&creative_id=__CID__&creative_name=__CID_NAME__&idfa=__IDFA__&idfa_md5=__IDFA_MD5__&caid=__CAID__&reqid=__REQ_ID__&trace_id=__TRACE_ID__"
        + "&click_time=__TS__&client_ip=__IP__&ua=__UA__&resource_id=__RID__&os=__OS__&os_version=__OS_VERSION__&model=__MODEL__"
        + "&advertiser_id=__USER_ID__&bchannel=__BCHANNEL__&behavior=__BEHAVIOR__";

    private static final Map<String, String> CONVERSIONS = Map.of(
        "active", EventCodes.APP_ACTIVATE,
        "register", EventCodes.APP_REGISTER,
        "wake_up", EventCodes.APP_FIRST_WAKE_UP,
        "retain", EventCodes.APP_RETENTION_1,
        "pay", EventCodes.APP_PAY,
        "key_behavior", EventCodes.APP_KEY_ACTION,
        "eft_login", EventCodes.APP_ACTIVE
    );

    public TrackProcessorKaboss(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("kaboss", debugPrintService, appServer);
    }

    @Override
    protected boolean supports(String event) {
        return EventCodes.CLICK.equals(event);
    }

    @Override
    protected List<String> trackCodeKeys() {
        return List.of("bchannel");
    }

    @Override
    protected String entryUrl(Entry entry) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("__BEHAVIOR__", "CLICK");
        values.put("__BCHANNEL__", entry.code("bchannel"));
        values.put("__REQ_ID__", entry.eventId());
        // 必填的广告点击 ID,媒体不给时用平台的点击 ID 顶上,保证每次点击唯一
        values.put("__TRACE_ID__", entry.query("click_id") != null ? entry.query("click_id") : entry.eventId());
        values.put("__CALLBACK_URL__", entry.callbackUrl());
        values.put("__TS__", entry.query("ts"));
        values.put("__IMEI__", entry.query("imei"));
        values.put("__IMEI_MD5__", entry.query("imei_md5"));
        values.put("__OAID__", entry.query("oaid"));
        values.put("__OAID_MD5__", entry.query("oaid_md5"));
        values.put("__ANDROID_ID__", entry.query("android_id"));
        values.put("__ANDROID_ID_MD5__", entry.query("android_id_md5"));
        values.put("__IDFA__", entry.query("idfa"));
        values.put("__IDFA_MD5__", entry.query("idfa_md5"));
        values.put("__CAID__", PerfQueries.caidJson(entry.queries()));
        values.put("__IP__", entry.query("ip") != null ? entry.query("ip") : entry.query("ipv6"));
        values.put("__UA__", entry.query("ua"));
        // 必填,取值 ios 或 android
        values.put("__OS__", entry.os() == Os.IOS ? "ios" : "android");
        values.put("__OS_VERSION__", entry.query("os_v"));
        values.put("__MODEL__", entry.query("model"));
        return fillTemplate(TEMPLATE, values);
    }

    @Override
    protected boolean accepted(PerfHttp.Response response) {
        return response.isOkWith("code", "0");
    }

    @Override
    protected String conversionEvent(Map<String, String> queries) {
        return conversion(queries, "conv_type", CONVERSIONS, null);
    }

    @Override
    protected String amountKey() {
        return "conv_amount";
    }

}
