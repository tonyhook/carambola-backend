package cc.tonyhook.carambola.backend.service.perf;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.SerialnoRepository;
import cc.tonyhook.carambola.backend.entity.perf.Serialno;
import jakarta.transaction.Transactional;

@Service
public class SerialnoService {

    private static final Long SERIALNO_BASE = 0x10000000L;

    private final SerialnoRepository serialnoRepository;

    public SerialnoService(SerialnoRepository serialnoRepository) {
        this.serialnoRepository = serialnoRepository;
    }

    @Transactional
    public String nextSerialno() {
        Serialno serialno = serialnoRepository.saveAndFlush(new Serialno());

        return Long.toHexString(SERIALNO_BASE + serialno.getId()).toUpperCase();
    }

}
