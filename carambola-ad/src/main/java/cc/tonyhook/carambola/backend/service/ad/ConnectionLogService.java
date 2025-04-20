package cc.tonyhook.carambola.backend.service.ad;

import java.util.List;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.ConnectionLogRepository;
import cc.tonyhook.carambola.backend.entity.ad.ConnectionLog;
import jakarta.transaction.Transactional;

@Service
public class ConnectionLogService {

    private final ConnectionLogRepository connectionLogRepository;

    public ConnectionLogService(ConnectionLogRepository connectionLogRepository) {
        this.connectionLogRepository = connectionLogRepository;
    }

    public List<ConnectionLog> getConnectionLogList() {
        List<ConnectionLog> connectionLogList = connectionLogRepository.findAll();

        return connectionLogList;
    }

    public ConnectionLog getConnectionLog(Integer id) {
        ConnectionLog connectionLog = connectionLogRepository.findById(id).orElse(null);

        return connectionLog;
    }

    public ConnectionLog addConnectionLog(ConnectionLog newConnectionLog) {
        ConnectionLog updatedConnectionLog = connectionLogRepository.save(newConnectionLog);

        return updatedConnectionLog;
    }

    public void updateConnectionLog(Integer id, ConnectionLog newConnectionLog) {
        connectionLogRepository.save(newConnectionLog);
    }

    @Transactional
    public void removeConnectionLog(Integer id) {
        ConnectionLog deletedConnectionLog = connectionLogRepository.findById(id).orElse(null);

        connectionLogRepository.delete(deletedConnectionLog);
    }

}
