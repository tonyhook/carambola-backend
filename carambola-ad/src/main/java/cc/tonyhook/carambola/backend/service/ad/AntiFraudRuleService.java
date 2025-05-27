package cc.tonyhook.carambola.backend.service.ad;

import java.util.List;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.AntiFraudRuleRepository;
import cc.tonyhook.carambola.backend.entity.ad.AntiFraudRule;
import jakarta.transaction.Transactional;

@Service
public class AntiFraudRuleService {

    private final AntiFraudRuleRepository antiFraudRuleRepository;

    public AntiFraudRuleService(AntiFraudRuleRepository antiFraudRuleRepository) {
        this.antiFraudRuleRepository = antiFraudRuleRepository;
    }

    public List<AntiFraudRule> getAntiFraudRuleList(String username) {
        List<AntiFraudRule> antiFraudRuleList = antiFraudRuleRepository.findAll();
        return antiFraudRuleList;
    }

    public AntiFraudRule getAntiFraudRule(String username, Integer id) {
        if (username == null) {
            return antiFraudRuleRepository.findById(id).orElse(null);
        } else {
            return null;
        }
    }

    public AntiFraudRule getAntiFraudRule(String username, String code) {
        return antiFraudRuleRepository.findTopByCode(code);
    }

    public AntiFraudRule addAntiFraudRule(String username, AntiFraudRule newAntiFraudRule) {
        if (newAntiFraudRule != null && username == null) {
            AntiFraudRule updatedAntiFraudRule = antiFraudRuleRepository.save(newAntiFraudRule);

            return updatedAntiFraudRule;
        } else {
            return null;
        }
    }

    public AntiFraudRule updateAntiFraudRule(String username, AntiFraudRule targetAntiFraudRule, AntiFraudRule newAntiFraudRule) {
        if (targetAntiFraudRule != null && newAntiFraudRule != null && username == null) {
            AntiFraudRule updatedAntiFraudRule = antiFraudRuleRepository.save(newAntiFraudRule);

            return updatedAntiFraudRule;
        } else {
            return null;
        }
    }

    @Transactional
    public AntiFraudRule removeAntiFraudRule(String username, AntiFraudRule targetAntiFraudRule) {
        if (targetAntiFraudRule != null && username == null) {
            antiFraudRuleRepository.delete(targetAntiFraudRule);

            return targetAntiFraudRule;
        } else {
            return null;
        }
    }

}
