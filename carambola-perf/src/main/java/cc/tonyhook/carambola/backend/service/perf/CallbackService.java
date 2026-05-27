package cc.tonyhook.carambola.backend.service.perf;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.perf.CallbackRepository;
import cc.tonyhook.carambola.backend.entity.perf.Callback;

@Service
public class CallbackService {

    private final CallbackRepository callbackRepository;
    private final SerialnoService serialnoService;

    public CallbackService(CallbackRepository callbackRepository, SerialnoService serialnoService) {
        this.callbackRepository = callbackRepository;
        this.serialnoService = serialnoService;
    }

    public List<Callback> getCallbackList() {
        List<Callback> callbackList = callbackRepository.findAll();

        return callbackList;
    }

    public Callback getCallback(Integer id) {
        Callback callback = callbackRepository.findById(id).orElse(null);

        return callback;
    }

    public Callback addCallback(Callback newCallback) {
        if (newCallback.getTime() == null) {
            newCallback.setTime(new Timestamp(System.currentTimeMillis()));
        }
        if (newCallback.getSerialno() == null) {
            newCallback.setSerialno(serialnoService.nextSerialno());
        }
        Callback updatedCallback = callbackRepository.save(newCallback);

        return updatedCallback;
    }

}
