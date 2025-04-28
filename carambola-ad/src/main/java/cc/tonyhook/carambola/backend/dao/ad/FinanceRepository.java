package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Finance;

public interface FinanceRepository extends JpaRepository<Finance, Integer> {

    List<Finance> findByTimeBetween(Timestamp start, Timestamp end);

}
