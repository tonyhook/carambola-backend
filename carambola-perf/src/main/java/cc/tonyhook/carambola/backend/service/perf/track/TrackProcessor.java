package cc.tonyhook.carambola.backend.service.perf.track;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.tonyhook.carambola.backend.entity.perf.ClientChannelRoute;
import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.entity.perf.EventCodes;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfHttp;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries;

/**
 * 监测方处理器。每个监测方只声明自己与众不同的部分:
 *
 * 出站(入口事件上报):入口事件是媒体打进来、由我们转发给监测方的那个事件,
 * 通常是点击,也可能是展示或任何其它事件,由 {@link #supports} 界定
 * - {@link #supports}:接收哪些事件,其余返回 UNSUPPORTED,请求不发出
 * - {@link #trackCodeKeys}:渠道路由上 trackCode 的各段含义,须与 Track.protocolKey 一致
 * - {@link #entryUrl}:上报地址。第三方模板统一走 {@link #fillTemplate}
 * - {@link #accepted}:什么样的响应算成功
 *
 * 入站(转化回传):
 * - {@link #conversionEvent}:对方的转化类型 → 平台事件码,认不出一律拒收
 * - {@link #amountKey}:金额参数,单位须为分
 *
 * 回传地址、HTTP、编码、异常处理都在这里,子类不再各写一份。
 */
public abstract class TrackProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackProcessor.class);

    // 第三方模板里的宏:__IMEI__、__caid_ver1__ 这类以双下划线包起来的标识
    private static final Pattern TEMPLATE_MACRO = Pattern.compile("__[A-Za-z0-9]+(?:_[A-Za-z0-9]+)*__");

    protected final PerfDebugPrintService debugPrintService;

    private final String name;

    private final String appServer;

    protected TrackProcessor(String name, PerfDebugPrintService debugPrintService, String appServer) {
        this.name = name;
        this.debugPrintService = debugPrintService;
        this.appServer = appServer;
    }

    public String getName() {
        return name;
    }

    protected abstract boolean supports(String event);

    protected abstract List<String> trackCodeKeys();

    protected abstract String entryUrl(Entry entry);

    protected boolean accepted(PerfHttp.Response response) {
        return response.isOk();
    }

    protected abstract String conversionEvent(Map<String, String> queries);

    protected String amountKey() {
        return null;
    }

    public final DeliveryResult event(Event event, ClientChannelRoute route, String deliveryToken) {
        if (!supports(event.getEvent())) {
            return DeliveryResult.UNSUPPORTED;
        }

        String url;
        try {
            url = buildEntryUrl(event, route, deliveryToken);
        } catch (MisconfiguredRouteException e) {
            LOGGER.warn(
                "Track route misconfigured: track={}, route={}, reason={}",
                name, route.getId(), e.getMessage());
            return DeliveryResult.FAILED;
        } catch (RuntimeException e) {
            LOGGER.warn("Cannot build track url: track={}, event={}, route={}", name, event.getId(), route.getId(), e);
            return DeliveryResult.FAILED;
        }

        debugPrintService.println("   eventB:" + url);
        PerfHttp.Response response;
        try {
            response = PerfHttp.get(url);
        } catch (RuntimeException e) {
            LOGGER.warn("Invalid track url: track={}, event={}, url={}", name, event.getId(), url, e);
            return DeliveryResult.FAILED;
        }
        debugPrintService.println("   eventC:" + response);
        return DeliveryResult.of((response != null) && accepted(response));
    }

    // 只拼地址不发送;配置缺失时抛 MisconfiguredRouteException
    final String buildEntryUrl(Event event, ClientChannelRoute route, String deliveryToken) {
        Entry entry = new Entry(event, parseTrackCode(route.getTrackCode()), deliveryToken, callbackUrl(deliveryToken));
        return entryUrl(entry);
    }

    public final Event callback(Map<String, String> queries) {
        String event = conversionEvent(queries);
        if (event == null) {
            return null;
        }

        Event callback = new Event();
        callback.setEvent(event);
        String amountKey = amountKey();
        if ((amountKey != null) && PerfQueries.isValid(queries, amountKey)) {
            try {
                callback.setAmount(new BigDecimal(queries.get(amountKey).trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        callback.setQueries(queries);
        return callback;
    }

    // 回传地址统一由 davidia_track 与 davidia_delivery 构成,OpenPerfController 凭后者反查原始入口事件
    protected String callbackUrl(String deliveryToken) {
        String callbackUrl = trimTrailingSlash(appServer) + "/api/open/callback?davidia_track=" + name;
        if (deliveryToken != null) {
            callbackUrl += "&davidia_delivery=" + encode(deliveryToken);
        }
        return callbackUrl;
    }

    /**
     * 按表把对方的转化类型翻成平台事件码。参数缺失时取 whenAbsent(协议规定了缺省语义才给,否则传 null);
     * 取值不在表里一律拒收,不猜。
     */
    protected static String conversion(Map<String, String> queries, String key, Map<String, String> types, String whenAbsent) {
        if (!PerfQueries.isValid(queries, key)) {
            return whenAbsent;
        }
        return types.get(queries.get(key).trim());
    }

    // 自有协议直接携带平台事件码;不认识的码拒收,否则会在报表里落成一行无名事件
    protected static String ownProtocolEvent(Map<String, String> queries) {
        if (!PerfQueries.isValid(queries, "davidia_event")) {
            return null;
        }
        String event = queries.get("davidia_event").trim();
        if (!EventCodes.all().contains(event) && !EventCodes.isCustom(event)) {
            return null;
        }
        return event;
    }

    /**
     * 第三方监测模板的宏替换。模板里的每个宏按 values 取值并做 URL 编码;
     * 取不到值的宏替换为空串,不把“__IMEI__”这样的裸宏当设备号送出去。
     */
    protected static String fillTemplate(String template, Map<String, String> values) {
        Matcher matcher = TEMPLATE_MACRO.matcher(template);
        StringBuilder url = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group());
            matcher.appendReplacement(url, Matcher.quoteReplacement(value == null ? "" : encode(value)));
        }
        matcher.appendTail(url);
        return url.toString();
    }

    protected static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    protected static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private Map<String, String> parseTrackCode(String trackCode) {
        List<String> keys = trackCodeKeys();
        if (StringUtils.isBlank(trackCode)) {
            throw new MisconfiguredRouteException("empty track code, expected " + keys);
        }

        // 只有一段时整串就是这一段(可能是带竖线的完整地址),不拆
        String[] segments = keys.size() == 1 ? new String[] { trackCode } : trackCode.split("\\|", -1);
        if (segments.length < keys.size()) {
            throw new MisconfiguredRouteException("track code has " + segments.length + " segments, expected " + keys);
        }

        Map<String, String> code = new LinkedHashMap<String, String>();
        for (int i = 0; i < keys.size(); i++) {
            code.put(keys.get(i), StringUtils.trimToNull(segments[i]));
        }
        return code;
    }

    /**
     * 一次入口事件上报的上下文。取值方法都返回“有效值或 null”,交给 fillTemplate 统一处理缺失。
     */
    protected static final class Entry {

        private final Event event;
        private final Map<String, String> trackCode;
        private final String deliveryToken;
        private final String callbackUrl;

        Entry(Event event, Map<String, String> trackCode, String deliveryToken, String callbackUrl) {
            this.event = event;
            this.trackCode = trackCode;
            this.deliveryToken = deliveryToken;
            this.callbackUrl = callbackUrl;
        }

        public Event event() {
            return event;
        }

        public boolean is(String eventCode) {
            return eventCode.equals(event.getEvent());
        }

        public String eventId() {
            return String.valueOf(event.getId());
        }

        public Map<String, String> queries() {
            return event.getQueries();
        }

        public String query(String key) {
            return PerfQueries.get(event.getQueries(), key);
        }

        public PerfQueries.Os os() {
            return PerfQueries.os(event.getQueries());
        }

        public String deliveryToken() {
            return deliveryToken;
        }

        public String callbackUrl() {
            return callbackUrl;
        }

        // 缺了就无法上报的段:缺失时整条路由按配置错误处理
        public String code(String key) {
            String value = trackCode.get(key);
            if (value == null) {
                throw new MisconfiguredRouteException("track code segment '" + key + "' is empty");
            }
            return value;
        }

        public String optionalCode(String key) {
            return trackCode.get(key);
        }

    }

    protected static final class MisconfiguredRouteException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        MisconfiguredRouteException(String message) {
            super(message);
        }

    }

}
