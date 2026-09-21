package cc.tonyhook.carambola.backend.dao.perf;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.perf.EventHourlyProgress;

public interface EventHourlyProgressRepository extends JpaRepository<EventHourlyProgress, Integer> {

}
