package cc.tonyhook.carambola.backend.service.perf.track;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries;

/**
 * 自有协议。与第三方模板不同:上报地址由监测方给(trackCode 第一段),取不到值的参数整个不发,
 * 入口事件带来的其余参数原样透传。
 */
@Component("trackDavidia")
public class TrackProcessorDavidia extends TrackProcessor {

    private static final Pattern MACRO = Pattern.compile("__[A-Z0-9_]+__");

    // 回调链接整条转义后下发。末尾的 encode() 会把 % 再转一次,
    // 因此先占位,等 encode() 之后再把转义好的链接换进来
    private static final String CALLBACK_PLACEHOLDER = "davidiacallbackplaceholder0a1b2c3d";

    private static final Set<String> RESERVED_PARAMS = Set.of(
        "davidia_media",
        "davidia_id",
        "davidia_event",
        "davidia_callback",
        "davidia_amount",
        "davidia_track",
        "davidia_delivery"
    );

    public TrackProcessorDavidia(PerfDebugPrintService debugPrintService, @Value("${app.server}") String appServer) {
        super("davidia", debugPrintService, appServer);
    }

    @Override
    protected boolean supports(String event) {
        return true;
    }

    @Override
    protected List<String> trackCodeKeys() {
        return List.of("url", "davidia_id");
    }

    @Override
    protected String entryUrl(Entry entry) {
        String eventUrl = entry.code("url");
        String davidiaId = entry.code("davidia_id");

        // 上报地址可以是带宏的模板(协议第 3 节宏表),模板里已经点名的参数由宏替换填值,
        // 平台不再重复追加同名参数
        Map<String, String> macros = platformMacros(entry, davidiaId);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(eventUrl);
        MultiValueMap<String, String> template = UriComponentsBuilder.fromUriString(eventUrl).build().getQueryParams();
        for (String key : template.keySet()) {
            String value = template.getFirst(key);
            String resolved = resolveMacros(value, macros, entry.queries());
            if (Objects.equals(value, resolved)) {
                continue;
            }
            if (resolved == null) {
                // 宏取不到值,与其把“__OAID__”当设备号发出去,不如整个参数不发
                builder.replaceQueryParam(key);
            } else {
                builder.replaceQueryParam(key, resolved);
            }
        }
        if (!template.containsKey("davidia_id")) {
            builder.queryParam("davidia_id", davidiaId);
        }
        if (!template.containsKey("davidia_event")) {
            builder.queryParam("davidia_event", entry.event().getEvent());
        }
        if (!template.containsKey("davidia_callback")) {
            builder.queryParam("davidia_callback", CALLBACK_PLACEHOLDER);
        }
        if (entry.queries() != null) {
            for (Map.Entry<String, String> query : entry.queries().entrySet()) {
                if (RESERVED_PARAMS.contains(query.getKey()) || template.containsKey(query.getKey())) {
                    continue;
                }
                builder.queryParam(query.getKey(), query.getValue());
            }
        }

        return builder.build()
            .encode(StandardCharsets.UTF_8)
            .toUriString()
            .replace(CALLBACK_PLACEHOLDER, encode(entry.callbackUrl()));
    }

    @Override
    protected String conversionEvent(Map<String, String> queries) {
        return ownProtocolEvent(queries);
    }

    @Override
    protected String amountKey() {
        return "davidia_amount";
    }

    // 平台自己生成的宏。其余宏由宏名推导参数名取值,见 macroValue
    private Map<String, String> platformMacros(Entry entry, String davidiaId) {
        Map<String, String> macros = new LinkedHashMap<String, String>();
        macros.put("__DAVIDIA_ID__", davidiaId);
        macros.put("__DAVIDIA_EVENT__", entry.event().getEvent());
        macros.put("__DAVIDIA_DELIVERY__", entry.deliveryToken());
        macros.put("__DAVIDIA_CALLBACK__", CALLBACK_PLACEHOLDER);
        macros.put("__DAVIDIA_AMOUNT__", entry.event().getAmount() == null ? null : entry.event().getAmount().toPlainString());
        return macros;
    }

    // 返回替换后的值;值里有宏但取不到值时返回 null,表示这个参数应当丢弃
    private String resolveMacros(String value, Map<String, String> macros, Map<String, String> queries) {
        if (value == null) {
            return null;
        }

        Matcher matcher = MACRO.matcher(value);
        StringBuilder resolved = new StringBuilder();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String replacement = macroValue(matcher.group(), macros, queries);
            if (StringUtils.isBlank(replacement)) {
                return null;
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        if (!found) {
            return value;
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    // 协议第 3 节的宏名与查询参数名一一对应(__参数名大写__),参数名即平台词表(PerfQueries)
    private String macroValue(String macro, Map<String, String> macros, Map<String, String> queries) {
        if (macros.containsKey(macro)) {
            return macros.get(macro);
        }
        String key = StringUtils.toRootLowerCase(macro.substring(2, macro.length() - 2));
        return PerfQueries.get(queries, key);
    }

}
