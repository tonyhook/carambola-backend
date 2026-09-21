package cc.tonyhook.carambola.backend.entity.perf;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// 小时汇总的水位,只有一行:rolledUntil 之前的整点都已汇总进 perf_event_hourly。
@Entity
@Table(name = "perf_event_hourly_progress")
public class EventHourlyProgress {

    public static final Integer SINGLETON_ID = 1;

    @Id
    private Integer id;

    private Timestamp rolledUntil;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Timestamp getRolledUntil() {
        return this.rolledUntil;
    }

    public void setRolledUntil(Timestamp rolledUntil) {
        this.rolledUntil = rolledUntil;
    }

}
