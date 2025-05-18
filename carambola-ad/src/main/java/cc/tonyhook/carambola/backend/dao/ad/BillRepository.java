package cc.tonyhook.carambola.backend.dao.ad;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Bill;

public interface BillRepository extends JpaRepository<Bill, Integer> {

    List<Bill> findByClientPortInAndDateBetween(List<Integer> clientPortIdList, Timestamp start, Timestamp end);
    List<Bill> findByClientPortInAndStatusAndDateBetween(List<Integer> clientPortIdList, Integer status, Timestamp start, Timestamp end);

}
