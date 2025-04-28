package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Performance;

public interface PerformanceRepository extends JpaRepository<Performance, Integer> {

    List<Performance> findByTimeBetween(Timestamp start, Timestamp end);

}
