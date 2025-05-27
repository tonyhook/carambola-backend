package cc.tonyhook.carambola.backend.dao.ad;

import org.springframework.data.jpa.repository.JpaRepository;

import cc.tonyhook.carambola.backend.entity.ad.AntiFraudRule;

public interface AntiFraudRuleRepository extends JpaRepository<AntiFraudRule, Integer> {

    AntiFraudRule findTopByCode(String code);

}
