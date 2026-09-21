package cc.tonyhook.carambola.backend.service.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import cc.tonyhook.carambola.backend.service.perf.CountService.RolledRange;

class CountServiceTest {

    @Test
    void wholeHoursBeforeWatermarkAreRolled() {
        // 东八区的 7 天:本地零点即 UTC 16:00
        RolledRange rolled = CountService.rolledRange(
            at("2026-09-16T16:00:00Z"), at("2026-09-23T16:00:00Z"), 480, at("2026-09-23T06:00:00Z"));

        assertThat(rolled.start()).isEqualTo(at("2026-09-16T16:00:00Z"));
        assertThat(rolled.end()).isEqualTo(at("2026-09-23T06:00:00Z"));
    }

    @Test
    void partialHoursAtBothEndsAreLeftToRawEvents() {
        RolledRange rolled = CountService.rolledRange(
            at("2026-09-22T01:30:00Z"), at("2026-09-22T05:10:00Z"), 0, at("2026-09-23T00:00:00Z"));

        assertThat(rolled.start()).isEqualTo(at("2026-09-22T02:00:00Z"));
        assertThat(rolled.end()).isEqualTo(at("2026-09-22T05:00:00Z"));
    }

    @Test
    void nothingIsRolledWithoutWholeHourInside() {
        assertThat(CountService.rolledRange(
            at("2026-09-22T01:10:00Z"), at("2026-09-22T01:50:00Z"), 0, at("2026-09-23T00:00:00Z"))).isNull();
        assertThat(CountService.rolledRange(
            at("2026-09-22T01:10:00Z"), at("2026-09-22T02:50:00Z"), 0, at("2026-09-23T00:00:00Z"))).isNull();
    }

    @Test
    void nothingIsRolledBeforeWatermarkReachesWindow() {
        assertThat(CountService.rolledRange(
            at("2026-09-22T00:00:00Z"), at("2026-09-23T00:00:00Z"), 0, null)).isNull();
        assertThat(CountService.rolledRange(
            at("2026-09-22T00:00:00Z"), at("2026-09-23T00:00:00Z"), 0, at("2026-09-21T00:00:00Z"))).isNull();
    }

    @Test
    void offsetsOffWholeHourFallBackToRawEvents() {
        assertThat(CountService.rolledRange(
            at("2026-09-22T00:00:00Z"), at("2026-09-23T00:00:00Z"), 330, at("2026-09-24T00:00:00Z"))).isNull();
        assertThat(CountService.rolledRange(
            at("2026-09-22T00:00:00Z"), at("2026-09-23T00:00:00Z"), -570, at("2026-09-24T00:00:00Z"))).isNull();
    }

    private static Timestamp at(String instant) {
        return Timestamp.from(Instant.parse(instant));
    }

}
