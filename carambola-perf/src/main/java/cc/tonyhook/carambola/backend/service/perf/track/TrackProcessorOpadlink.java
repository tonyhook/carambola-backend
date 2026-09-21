package cc.tonyhook.carambola.backend.service.perf.track;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfHttp;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Os;

/**
 * openinstall 通用广告平台接口文档。
 */
@Component("trackOpadlink")
public class TrackProcessorOpadlink extends TrackProcessor {

    private static final String TEMPLATE = "https://opadlink.com/ad/__ACTION__/__OP_AK__"
        + "?cid=__CID__&op_cc=__OP_CC__&os=__OS__&idfa=__IDFA__&oaid=__OAID__&caid=__CAID__&imei=__IMEI__&mac=__MAC__&ip=__IP__&ua=__UA__"
        + "&ad_clickid=__AD_CLICKID__";

    private static final Map<String, String> CONVERSIONS = Map.of(
        "active", EventCodes.APP_ACTIVATE,
        "register", EventCodes.APP_REGISTER,
        "retain_1", EventCodes.APP_RETENTION_1,
        "pay", EventCodes.APP_PAY
    );

    public TrackProcessorOpadlink(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("opadlink", debugPrintService, appServer);
    }

    @Override
    protected boolean supports(String event) {
        return EventCodes.CLICK.equals(event);
    }

    @Override
    protected List<String> trackCodeKeys() {
        return List.of("op_ak", "cid", "op_cc");
    }

    // 回传地址预置 type=active:对方原样回调时即为激活
    @Override
    protected String callbackUrl(String deliveryToken) {
        return super.callbackUrl(deliveryToken) + "&type=active";
    }

    @Override
    protected String entryUrl(Entry entry) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("__ACTION__", "click");
        values.put("__OP_AK__", entry.code("op_ak"));
        values.put("__CID__", entry.code("cid"));
        values.put("__OP_CC__", entry.optionalCode("op_cc"));
        values.put("__AD_CLICKID__", entry.callbackUrl());
        values.put("__IMEI__", entry.query("imei"));
        values.put("__OAID__", entry.query("oaid"));
        values.put("__IDFA__", entry.query("idfa"));
        values.put("__CAID__", entry.query("caid") != null ? entry.query("caid") : entry.query("caid1"));
        values.put("__MAC__", entry.query("mac"));
        values.put("__IP__", entry.query("ip"));
        values.put("__UA__", entry.query("ua"));
        // 1 表示 iOS,0 表示安卓
        values.put("__OS__", entry.os() == Os.IOS ? "1" : "0");
        return fillTemplate(TEMPLATE, values);
    }

    @Override
    protected boolean accepted(PerfHttp.Response response) {
        return response.isOkWith("code", "0");
    }

    // 协议没有规定缺省语义;沿用线上一直以来的“缺 type 按注册”
    @Override
    protected String conversionEvent(Map<String, String> queries) {
        return conversion(queries, "type", CONVERSIONS, EventCodes.APP_REGISTER);
    }

    @Override
    protected String amountKey() {
        return "pay_amount";
    }

}
