package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.ConnectionLog;

public interface ConnectionLogRepository extends JpaRepository<ConnectionLog, Integer> {

    List<ConnectionLog> findByConnectionInOrderByTime(List<Connection> connectionLogList);

}
