package cc.tonyhook.carambola.backend.service.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventCallbackUrlPartitionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T00:00:00Z");

    @Test
    void firstPartitioningTakesInExistingRowsAndReservesAhead() {
        assertThat(EventCallbackUrlPartitionService.boundsToAdd(null, 14_700_000L))
            .containsExactly(15_000_000L, 20_000_000L, 25_000_000L);
        assertThat(EventCallbackUrlPartitionService.boundsToAdd(null, 0L))
            .containsExactly(5_000_000L, 10_000_000L, 15_000_000L);
    }

    @Test
    void nothingIsAddedWhileEnoughPartitionsAreAhead() {
        assertThat(EventCallbackUrlPartitionService.boundsToAdd(25_000_000L, 14_999_999L)).isEmpty();
    }

    @Test
    void crossingIntoTheNextPartitionAddsOneMore() {
        assertThat(EventCallbackUrlPartitionService.boundsToAdd(25_000_000L, 15_000_000L))
            .containsExactly(30_000_000L);
    }

    @Test
    void stalledMaintenanceCatchesUpInOneGo() {
        assertThat(EventCallbackUrlPartitionService.boundsToAdd(25_000_000L, 31_000_000L))
            .containsExactly(30_000_000L, 35_000_000L, 40_000_000L, 45_000_000L);
    }

    @Test
    void partitionIsClosedOnceAllItsIdsAreIssued() {
        assertThat(EventCallbackUrlPartitionService.isClosed(15_000_000L, 14_999_999L)).isFalse();
        assertThat(EventCallbackUrlPartitionService.isClosed(15_000_000L, 15_000_000L)).isTrue();
    }

    @Test
    void partitionExpiresOnlyWhenItsNewestEventIsPastRetention() {
        assertThat(EventCallbackUrlPartitionService.isExpired(at("2026-09-30T23:59:59Z"), NOW)).isTrue();
        assertThat(EventCallbackUrlPartitionService.isExpired(at("2026-10-01T00:00:01Z"), NOW)).isFalse();
        assertThat(EventCallbackUrlPartitionService.isExpired(null, NOW)).isFalse();
    }

    @Test
    void definitionsEndWithCatchAll() {
        assertThat(EventCallbackUrlPartitionService.definitions(List.of(15_000_000L, 20_000_000L)))
            .isEqualTo("PARTITION p15000000 VALUES LESS THAN (15000000), "
                + "PARTITION p20000000 VALUES LESS THAN (20000000), "
                + "PARTITION pmax VALUES LESS THAN MAXVALUE");
    }

    private static Timestamp at(String instant) {
        return Timestamp.from(Instant.parse(instant));
    }

}
