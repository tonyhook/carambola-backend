package cc.tonyhook.carambola.backend.service.perf.media;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import cc.tonyhook.carambola.backend.service.perf.DeliveryResult;
import cc.tonyhook.carambola.backend.service.perf.PerfDebugPrintService;
import cc.tonyhook.carambola.backend.service.perf.PerfHttp;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Caid;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Os;

/**
 * 媒体处理器。每个媒体只声明自己与众不同的部分,同一件事在所有媒体里都落在同名的方法上,
 * 便于横向比对:
 *
 * 入站(入口事件监测):
 * - {@link #eventMacros}:监测链接里 平台参数名 → 媒体宏,决定入口事件带来哪些平台词表字段
 * - {@link #osAliases}:媒体 os 的特有取值(大小写不敏感的名称已统一识别)
 * - {@link #collectCaid}:媒体的 caid 取值怎么拆成 版本号 → caid/caid_md5
 * - {@link #callbackUrl} + {@link #callbackTokens}:媒体带回的是裸标识而不是完整回传地址时,
 *   标识在哪个参数里、包成哪个转化接口地址
 *
 * 刻意不留“其余归一”这样的兜底钩子:归一的每一类都得有自己的名字,新媒体要归一的东西
 * 对不上任何一个钩子时,在这里加一个名字明确的钩子,不要塞进别处,否则同一件事又会在
 * 各媒体里长出不同的写法。
 *
 * 出站(转化回传):
 * - {@link #sendConversion}:平台事件码 → 媒体转化类型、签名、发送。没有对应类型返回 UNSUPPORTED
 *
 * davidia_ 信封(davidia_id、davidia_event、davidia_amount、davidia_callback)、未替换宏的剔除、
 * 监测链接生成、异常处理都在这里,子类不再各写一份。词表字段本身的归一(os、caid)属于
 * PerfQueries:这里只负责把上面那几个钩子的声明交给它。
 */
public abstract class MediaProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaProcessor.class);

    protected final PerfDebugPrintService debugPrintService;

    private final String name;

    private final String appServer;

    protected MediaProcessor(String name, PerfDebugPrintService debugPrintService, String appServer) {
        this.name = name;
        this.debugPrintService = debugPrintService;
        this.appServer = appServer;
    }

    public String getName() {
        return name;
    }

    protected abstract Map<String, String> eventMacros();

    protected Map<String, Os> osAliases() {
        return Map.of();
    }

    /**
     * 把媒体的 caid 取值逐条塞进 caid。排序、配对、写成词表的 caid1/caid1_md5/caid1_v
     * 由 {@link PerfQueries#normalizeCaid} 做,子类只管解析自己的格式。
     * 媒体已按词表写法给 caid1/caid2 时不必覆写。
     */
    protected void collectCaid(Map<String, String> queries, Caid caid) {
    }

    // 媒体的转化接口地址。媒体带回裸标识而不是完整回传地址时,与 callbackTokens 配对声明
    protected String callbackUrl() {
        return null;
    }

    /**
     * 媒体把回传标识放在哪个参数里 → 它在 {@link #callbackUrl} 上的参数名。
     * 按声明顺序取第一个有值的,拼成回传地址存进 davidia_callback。
     * 媒体直接给完整回传地址(宏映射到 davidia_callback)时不必覆写。
     */
    protected Map<String, String> callbackTokens() {
        return Map.of();
    }

    protected abstract DeliveryResult sendConversion(Event conversion, Event entry);

    public final Event event(Map<String, String> queries) {
        if (!PerfQueries.isValid(queries, "davidia_id") || !PerfQueries.isValid(queries, "davidia_event")) {
            return null;
        }

        Event event = new Event();
        event.setMedia(name);
        event.setMediaCode(queries.get("davidia_id"));
        event.setEvent(queries.get("davidia_event"));

        removeUnreplacedMacros(queries);
        PerfQueries.normalizeOs(queries, osAliases());
        PerfQueries.normalizeCaid(queries, caid(queries));
        normalizeCallback(queries);

        if (PerfQueries.isValid(queries, "davidia_amount")) {
            try {
                event.setAmount(new BigDecimal(queries.get("davidia_amount").trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        event.setQueries(queries);
        return event;
    }

    /**
     * 把 conversion 回传给 entry 所属的媒体。entry 是当初打进来、由我们转发出去的那个入口事件,
     * 可能是点击,也可能是展示或任何其它事件;回传地址都在它进来时存进了 davidia_callback,
     * 没有它就无从回传。
     */
    public final DeliveryResult callback(Event conversion, Event entry) {
        if (!PerfQueries.isValid(entry.getQueries(), "davidia_callback")) {
            return DeliveryResult.FAILED;
        }

        try {
            return sendConversion(conversion, entry);
        } catch (RuntimeException e) {
            LOGGER.warn(
                "Cannot send conversion: media={}, conversion={}, event={}",
                name, conversion.getId(), entry.getId(), e);
            return DeliveryResult.FAILED;
        }
    }

    public String getEventUrl(String mediaCode, String mediaEvent) {
        StringBuilder url = new StringBuilder();
        url.append(eventServer()).append("/api/open/event?");
        url.append("davidia_media=").append(name);
        url.append("&davidia_id=").append(mediaCode);
        url.append("&davidia_event=").append(mediaEvent);
        for (Map.Entry<String, String> entry : eventMacros().entrySet()) {
            url.append("&").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return url.toString();
    }

    // 监测链接的服务地址,个别媒体对协议或端口有要求时覆盖
    protected String eventServer() {
        return trimTrailingSlash(appServer);
    }

    protected DeliveryResult get(String url, Predicate<PerfHttp.Response> accepted) {
        debugPrintService.println("callbackB:" + url);
        return deliver(PerfHttp.get(url), accepted);
    }

    protected DeliveryResult get(UriComponentsBuilder builder, Predicate<PerfHttp.Response> accepted) {
        return get(builder.build().encode(StandardCharsets.UTF_8).toUriString(), accepted);
    }

    protected DeliveryResult postJson(String url, String json, Predicate<PerfHttp.Response> accepted) {
        debugPrintService.println("callbackB:" + url + " " + json);
        return deliver(PerfHttp.postJson(url, json), accepted);
    }

    private DeliveryResult deliver(PerfHttp.Response response, Predicate<PerfHttp.Response> accepted) {
        debugPrintService.println("callbackC:" + response);
        return DeliveryResult.of((response != null) && accepted.test(response));
    }

    // 媒体方密钥按 Media.secretKey 声明的顺序竖线拼接存在渠道上,按下标取用。
    // 与 mediaCode 不同,它不参与渠道查找,也不会出现在监测链接里
    protected String mediaSecret(Event event, int index) {
        if ((event == null) || (event.getClientChannel() == null)) {
            return null;
        }

        String mediaSecret = event.getClientChannel().getMediaSecret();
        if (StringUtils.isBlank(mediaSecret)) {
            return null;
        }

        String[] secrets = mediaSecret.split("\\|", -1);
        if ((index < 0) || (index >= secrets.length)) {
            return null;
        }

        return StringUtils.trimToNull(secrets[index]);
    }

    protected static String query(Event event, String key) {
        return PerfQueries.get(event.getQueries(), key);
    }

    // 回传地址的构造器,子类只往上面加自己的转化参数,交给 get 统一编码发送
    protected static UriComponentsBuilder callbackBuilder(Event entry) {
        return UriComponentsBuilder.fromUriString(query(entry, "davidia_callback"));
    }

    // 回传地址上的某个参数。媒体的标识包在回传地址里时,回传阶段据此取回
    protected static String callbackParam(Event entry, String key) {
        String callback = query(entry, "davidia_callback");
        if (callback == null) {
            return null;
        }

        return UriComponentsBuilder.fromUriString(callback).build().getQueryParams().getFirst(key);
    }

    protected static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    // osAliases 之于 normalizeOs 是什么,这个之于 normalizeCaid 就是什么:
    // 只负责问媒体要它那一份解析结果,归一本身在 PerfQueries
    private Caid caid(Map<String, String> queries) {
        Caid caid = new Caid();
        collectCaid(queries, caid);
        return caid;
    }

    // PerfQueries.isValid 的未替换判断要求宏名为“__参数名大写__”,只有自有协议满足,
    // 因此解析前按宏表逐项剔除媒体原样返回的宏
    private void removeUnreplacedMacros(Map<String, String> queries) {
        for (Map.Entry<String, String> macro : eventMacros().entrySet()) {
            if (macro.getValue().equals(queries.get(macro.getKey()))) {
                queries.remove(macro.getKey());
            }
        }
    }

    // 回传地址不做 encode,与媒体透传进来的 davidia_callback 保持同一形态,回传时统一编码。
    // 源字段随即移除,避免同一个长 token 在 queries 里存两份
    private void normalizeCallback(Map<String, String> queries) {
        String callbackUrl = callbackUrl();
        if (callbackUrl == null) {
            return;
        }

        for (Map.Entry<String, String> token : callbackTokens().entrySet()) {
            if (!PerfQueries.isValid(queries, token.getKey())) {
                continue;
            }
            queries.put("davidia_callback", UriComponentsBuilder.fromUriString(callbackUrl)
                .queryParam(token.getValue(), queries.get(token.getKey()))
                .build()
                .toUriString());
            queries.remove(token.getKey());
            return;
        }
    }

    public Map<String, String> cleanQueries(Event event) {
        return cleanQueries(event.getQueries());
    }

    public Map<String, String> cleanQueries(Map<String, String> queries) {
        Map<String, String> cleanQueries = new LinkedHashMap<String, String>();
        if (queries == null) {
            return cleanQueries;
        }

        for (Map.Entry<String, String> entry : queries.entrySet()) {
            if (isCleanQuery(entry.getKey(), entry.getValue())) {
                cleanQueries.put(entry.getKey(), entry.getValue());
            }
        }

        return cleanQueries;
    }

    private boolean isCleanQuery(String key, String value) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return false;
        }
        if (key.startsWith("davidia_")) {
            return false;
        }
        for (String macro : eventMacros().values()) {
            if (value.contains(macro)) {
                return false;
            }
        }

        return true;
    }

}
