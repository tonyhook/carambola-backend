package cc.tonyhook.carambola.backend.dao.ad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.AntiFraud;

public interface AntiFraudRepository extends JpaRepository<AntiFraud, Integer> {

    List<AntiFraud> findByClientPort(Integer clientPortId);

}
