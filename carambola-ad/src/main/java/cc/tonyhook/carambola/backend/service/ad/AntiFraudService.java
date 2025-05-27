package cc.tonyhook.carambola.backend.service.ad;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import cc.tonyhook.carambola.backend.dao.ad.AntiFraudRepository;
import cc.tonyhook.carambola.backend.entity.ad.AntiFraud;
import jakarta.transaction.Transactional;

@Service
public class AntiFraudService {

    private final AntiFraudRepository antiFraudRepository;

    public AntiFraudService(AntiFraudRepository antiFraudRepository) {
        this.antiFraudRepository = antiFraudRepository;
    }

    public List<AntiFraud> getAntiFraudList(Authentication authentication, Integer clientPortId) {
        List<AntiFraud> antiFraudList = antiFraudRepository.findByClientPort(clientPortId);

        return antiFraudList;
    }

    public AntiFraud getAntiFraud(Integer id) {
        AntiFraud antiFraud = antiFraudRepository.findById(id).orElse(null);

        return antiFraud;
    }

    public AntiFraud addAntiFraud(AntiFraud newAntiFraud) {
        if (newAntiFraud != null) {
            AntiFraud updatedAntiFraud = antiFraudRepository.save(newAntiFraud);

            return updatedAntiFraud;
        } else {
            return null;
        }
    }

    public void updateAntiFraud(Integer id, AntiFraud newAntiFraud) {
        if (newAntiFraud != null) {
            antiFraudRepository.save(newAntiFraud);
        }
    }

    @Transactional
    public void removeAntiFraud(AntiFraud targetAntiFraud) {
        if (targetAntiFraud != null) {
            antiFraudRepository.delete(targetAntiFraud);
        }
    }

}
