package cc.tonyhook.carambola.backend.service.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Caid;
import cc.tonyhook.carambola.backend.service.perf.PerfQueries.Os;

class PerfQueriesTest {

    @Test
    void unreplacedOwnProtocolMacroIsInvalid() {
        Map<String, String> queries = Map.of("oaid", "__OAID__", "imei", " ", "idfa", "I");

        assertThat(PerfQueries.get(queries, "oaid")).isNull();
        assertThat(PerfQueries.get(queries, "imei")).isNull();
        assertThat(PerfQueries.get(queries, "idfa")).isEqualTo("I");
    }

    @Test
    void osIsNormalizedToCanonicalSpelling() {
        assertThat(normalizeOs("android", Map.of())).isEqualTo("Android");
        assertThat(normalizeOs("IOS", Map.of())).isEqualTo("iOS");
        assertThat(normalizeOs("harmony", Map.of())).isEqualTo("HarmonyOS");
        assertThat(normalizeOs("1", Map.of("1", Os.IOS))).isEqualTo("iOS");
        // 认不出的取值原样保留
        assertThat(normalizeOs("OTHERs", Map.of())).isEqualTo("OTHERs");
    }

    @Test
    void osIsInferredFromDeviceIdsWhenMissing() {
        assertThat(PerfQueries.inferOs(Map.of("os", "android", "idfa", "I"))).isEqualTo(Os.ANDROID);
        assertThat(PerfQueries.inferOs(Map.of("caid1", "C"))).isEqualTo(Os.IOS);
        assertThat(PerfQueries.inferOs(Map.of("oaid", "O"))).isEqualTo(Os.ANDROID);
        // 未替换的宏不算标识
        assertThat(PerfQueries.inferOs(Map.of("idfa", "__IDFA__"))).isNull();
        assertThat(PerfQueries.inferOs(Map.of("ip", "1.2.3.4"))).isNull();
    }

    @Test
    void splitCaidReplacesTheLoneRawCaid() {
        Caid caid = new Caid();
        caid.add("20230330", "old");
        caid.add("20250325", "new");
        caid.addMd5("20250325", "newmd5");
        // 版本号缺失的整条丢掉,不占 caid1/caid2 的位置
        caid.add(null, "noversion");
        Map<String, String> queries = queries("caid", "raw", "caid2", "stale");

        PerfQueries.normalizeCaid(queries, caid);

        assertThat(queries).containsEntry("caid1", "new")
            .containsEntry("caid1_md5", "newmd5")
            .containsEntry("caid1_v", "20250325")
            .containsEntry("caid2", "old")
            .containsEntry("caid2_v", "20230330")
            // 拆出了 caid1,没版本号的整串就删掉,否则 caidVersioned 会优先用它
            .doesNotContainKey("caid");
    }

    // 什么都没拆出来时不能动媒体给的整串 caid
    @Test
    void rawCaidSurvivesWhenNothingWasSplit() {
        Map<String, String> queries = queries("caid", "raw");

        PerfQueries.normalizeCaid(queries, new Caid());

        assertThat(queries).containsEntry("caid", "raw");
    }

    @Test
    void caidVersionedPrefersRawCaid() {
        assertThat(PerfQueries.caidVersioned(Map.of("caid", "20250325_a,20230330_b", "caid1", "x"))).isEqualTo("20250325_a,20230330_b");
        assertThat(PerfQueries.caidVersioned(Map.of("caid1", "a", "caid1_v", "20250325", "caid2", "b", "caid2_v", "20230330")))
            .isEqualTo("20250325_a,20230330_b");
        assertThat(PerfQueries.caidVersioned(Map.of("caid1", "a"))).isEqualTo("a");
        assertThat(PerfQueries.caidVersioned(Map.of())).isNull();
    }

    @Test
    void caidJsonPrefersSplitCaid() {
        assertThat(PerfQueries.caidJson(Map.of("caid1", "a", "caid1_v", "20250325", "caid2", "b", "caid2_v", "20230330", "caid", "raw")))
            .isEqualTo("[{\"caid\":\"a\",\"version\":\"20250325\"},{\"caid\":\"b\",\"version\":\"20230330\"}]");
        assertThat(PerfQueries.caidJson(Map.of("caid", "raw"))).isEqualTo("raw");
        assertThat(PerfQueries.caidJson(Map.of())).isNull();
    }

    private static Map<String, String> queries(String... pairs) {
        Map<String, String> queries = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            queries.put(pairs[i], pairs[i + 1]);
        }
        return queries;
    }

    private static String normalizeOs(String os, Map<String, Os> aliases) {
        Map<String, String> queries = new HashMap<String, String>(Map.of("os", os));
        PerfQueries.normalizeOs(queries, aliases);
        return queries.get("os");
    }

}
