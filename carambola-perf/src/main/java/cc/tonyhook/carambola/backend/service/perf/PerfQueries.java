package cc.tonyhook.carambola.backend.service.perf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 平台内部的入口事件参数词表。入口事件是媒体打进来、由我们转发出去的那个事件,
 * 通常是点击,也可能是展示或任何其它事件。
 *
 * 参数名以自有协议为准(MediaProcessorDavidia 的宏表):imei、oaid_md5、caid1、caid1_v、ip、ua、os、ts……
 * 各 MediaProcessor 负责把媒体自己的参数名和取值归一到这套写法,下游的去重、设备号识别、
 * TrackProcessor 只认这里的写法,不再各自兼容媒体差异。
 *
 * 取值约定:
 * - os 归一为 {@link Os} 的 canonical 值(Android、iOS……),认不出的原样保留
 * - caid 优先拆成 caid1/caid1_v、caid2/caid2_v(新版本在前);媒体只给一整串时存在 caid 里
 */
public final class PerfQueries {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 词表只留新旧两条 caid(caid1/caid2)
    private static final int CAID_LIMIT = 2;

    // 只在某一端存在的设备标识,用于 os 缺失时推断端
    private static final List<String> IOS_IDS = List.of(
        "idfa", "idfa_md5", "idfv", "idfv_md5", "caid", "caid1", "caid1_md5", "caid2", "caid2_md5");

    private static final List<String> ANDROID_IDS = List.of(
        "imei", "imei_md5", "oaid", "oaid_md5", "android_id", "android_id_md5");

    private PerfQueries() {
    }

    /**
     * 有值且不是原样返回的自有协议宏(“__参数名大写__”)。媒体自己的宏在入口已由
     * MediaProcessor 按宏表剔除,这里只需兜住自有协议。
     */
    public static boolean isValid(Map<String, String> queries, String key) {
        if (queries == null) {
            return false;
        }
        String value = queries.get(key);
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return !value.equals("__" + StringUtils.toRootUpperCase(key) + "__");
    }

    // 有效时返回值,否则返回 null
    public static String get(Map<String, String> queries, String key) {
        return isValid(queries, key) ? queries.get(key) : null;
    }

    public static Os os(Map<String, String> queries) {
        return Os.of(get(queries, "os"));
    }

    /**
     * os 认不出时按设备标识推断端:iOS 独有 idfa/idfv/caid,Android 独有 imei/oaid/android_id。
     * 都认不出返回 null。os 是选填宏的媒体据此补齐,不必各写一份。
     */
    public static Os inferOs(Map<String, String> queries) {
        Os os = os(queries);
        if (os != null) {
            return os;
        }

        for (String key : IOS_IDS) {
            if (isValid(queries, key)) {
                return Os.IOS;
            }
        }
        for (String key : ANDROID_IDS) {
            if (isValid(queries, key)) {
                return Os.ANDROID;
            }
        }
        return null;
    }

    /**
     * 把 os 归一为 canonical 值。aliases 是媒体特有的取值(如凤凰的 0/1),
     * 其余按名称大小写不敏感地识别;都认不出时保留原值。
     */
    public static void normalizeOs(Map<String, String> queries, Map<String, Os> aliases) {
        String value = get(queries, "os");
        if (value == null) {
            return;
        }
        Os os = aliases.containsKey(value) ? aliases.get(value) : Os.of(value);
        if (os != null) {
            queries.put("os", os.canonical());
        }
    }

    /**
     * 把 {@link Caid} 里收到的 CAID 按版本号从新到旧写成 caid1/caid2。明文与 md5 按版本号配对,
     * 否则同一个下标的 caid 与 caid_md5 会落到不同的设备上。
     *
     * 拆出了 caid1 就把没有版本号的整串 caid 删掉:两者并存时下游(caidVersioned)会优先用
     * 那一串,等于把版本号丢了。
     */
    public static void normalizeCaid(Map<String, String> queries, Caid caid) {
        List<Map<String, String>> caids = caid.newestFirst();
        for (int i = 0; (i < caids.size()) && (i < CAID_LIMIT); i++) {
            Map<String, String> item = caids.get(i);
            String prefix = "caid" + (i + 1);
            if (item.containsKey("caid")) {
                queries.put(prefix, item.get("caid"));
            }
            if (item.containsKey("caid_md5")) {
                queries.put(prefix + "_md5", item.get("caid_md5"));
            }
            queries.put(prefix + "_v", item.get("version"));
        }

        if (queries.containsKey("caid1")) {
            queries.remove("caid");
        }
    }

    /**
     * 完整 CAID 串:“版本号_CAID值”,多个版本以英文逗号拼接,新版本在前。
     * 媒体只给了一整串 caid 时原样返回。
     */
    public static String caidVersioned(Map<String, String> queries) {
        String caid = get(queries, "caid");
        if (caid != null) {
            return caid;
        }

        List<String> caids = new ArrayList<String>();
        for (int i = 1; i <= 2; i++) {
            String value = get(queries, "caid" + i);
            if (value == null) {
                continue;
            }
            String version = get(queries, "caid" + i + "_v");
            caids.add(version == null ? value : version + "_" + value);
        }
        return caids.isEmpty() ? null : String.join(",", caids);
    }

    /**
     * JSON 数组形式的 CAID:[{"caid":"…","version":"…"},…],新版本在前。
     * 没有拆开的 caid1/caid2 时退回媒体给的整串 caid。
     */
    public static String caidJson(Map<String, String> queries) {
        List<Map<String, String>> caids = new ArrayList<Map<String, String>>();
        for (int i = 1; i <= 2; i++) {
            String value = get(queries, "caid" + i);
            if (value == null) {
                continue;
            }
            Map<String, String> caid = new LinkedHashMap<String, String>();
            caid.put("caid", value);
            String version = get(queries, "caid" + i + "_v");
            if (version != null) {
                caid.put("version", version);
            }
            caids.add(caid);
        }
        if (caids.isEmpty()) {
            return get(queries, "caid");
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(caids);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 媒体带来的 CAID 集合,交给 {@link #normalizeCaid} 落成词表字段。调用方按版本号逐条塞进来,
     * 明文与 md5 分开给也能按版本号归并。版本号或取值为空的一律挡掉:未替换的宏切出来
     * 就是这种形状。
     */
    public static final class Caid {

        private final Map<String, Map<String, String>> byVersion = new LinkedHashMap<String, Map<String, String>>();

        public void add(String version, String caid) {
            put(version, "caid", caid);
        }

        public void addMd5(String version, String caidMd5) {
            put(version, "caid_md5", caidMd5);
        }

        private void put(String version, String key, String value) {
            if (StringUtils.isBlank(version) || StringUtils.isBlank(value)) {
                return;
            }

            String trimmedVersion = version.trim();
            byVersion.computeIfAbsent(trimmedVersion, v -> {
                Map<String, String> item = new LinkedHashMap<String, String>();
                item.put("version", v);
                return item;
            }).put(key, value.trim());
        }

        private List<Map<String, String>> newestFirst() {
            List<Map<String, String>> caids = new ArrayList<Map<String, String>>(byVersion.values());
            caids.sort((a, b) -> b.get("version").compareTo(a.get("version")));
            return caids;
        }

    }

    public enum Os {

        ANDROID("Android"),
        IOS("iOS"),
        HARMONY("HarmonyOS"),
        WINDOWS("Windows"),
        MACOS("MacOS");

        private final String canonical;

        Os(String canonical) {
            this.canonical = canonical;
        }

        public String canonical() {
            return canonical;
        }

        // 大小写不敏感地识别常见写法,认不出返回 null
        public static Os of(String value) {
            if (value == null) {
                return null;
            }
            switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "android":
                    return ANDROID;
                case "ios":
                    return IOS;
                case "harmony":
                case "harmonyos":
                case "ohos":
                    return HARMONY;
                case "windows":
                    return WINDOWS;
                case "macos":
                case "mac":
                    return MACOS;
                default:
                    return null;
            }
        }

    }

}
