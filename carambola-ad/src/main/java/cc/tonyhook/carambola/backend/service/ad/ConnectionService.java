package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.ClientPortRepository;
import cc.tonyhook.carambola.backend.dao.ad.ConnectionLogRepository;
import cc.tonyhook.carambola.backend.dao.ad.ConnectionRepository;
import cc.tonyhook.carambola.backend.dao.ad.VendorPortRepository;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.ConnectionLog;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import jakarta.transaction.Transactional;

@Service
public class ConnectionService {

    private final ClientPortRepository clientPortRepository;
    private final VendorPortRepository vendorPortRepository;
    private final ConnectionRepository connectionRepository;
    private final ConnectionLogRepository connectionLogRepository;

    public ConnectionService(
            ClientPortRepository clientPortRepository,
            VendorPortRepository vendorPortRepository,
            ConnectionRepository connectionRepository,
            ConnectionLogRepository connectionLogRepository
    ) {
        this.clientPortRepository = clientPortRepository;
        this.vendorPortRepository = vendorPortRepository;
        this.connectionRepository = connectionRepository;
        this.connectionLogRepository = connectionLogRepository;
    }

    public List<Connection> getConnectionList() {
        List<Connection> connectionList = connectionRepository.findAll();

        return connectionList;
    }

    public Connection getConnection(Integer id) {
        Connection connection = connectionRepository.findById(id).orElse(null);

        return connection;
    }

    public Connection addConnection(Connection newConnection) {
        Connection updatedConnection = connectionRepository.save(newConnection);

        return updatedConnection;
    }

    public void updateConnection(Integer id, Connection newConnection) {
        connectionRepository.save(newConnection);
    }

    @Transactional
    public void removeConnection(Integer id) {
        Connection deletedConnection = connectionRepository.findById(id).orElse(null);

        if (deletedConnection != null) {
            deletedConnection.setDeleted(true);
        }
    }

    public List<Integer> getPairedClientPort(Integer vendorPortId, Timestamp date) {
        VendorPort vendorPort = vendorPortRepository.findById(vendorPortId).orElse(null);
        if (vendorPort == null) {
            return new ArrayList<Integer>();
        }

        List<Connection> connectionList = new ArrayList<Connection>(connectionRepository.findByVendorPort(vendorPort).stream().filter(connection -> (connection.getValidFrom().equals(date) || connection.getValidFrom().before(date)) && connection.getValidTo().after(date)).toList());
        List<ConnectionLog> connectionLogList = connectionLogRepository.findByConnectionInOrderByTime(connectionList);
        Map<Integer, Long> lengthMap = new HashMap<Integer, Long>();

        for (Connection connection : connectionList) {
            List<Timestamp> starts = new ArrayList<Timestamp>();
            List<Timestamp> ends = new ArrayList<Timestamp>();
            Boolean enabled = false;
            List<ConnectionLog> connectionLogList1 = connectionLogList.stream().filter(connectionLog -> connectionLog.getConnection().getId().equals(connection.getId())).toList();
            for (ConnectionLog connectionLog : connectionLogList1) {
                if (connectionLog.getEnabled() != enabled) {
                    enabled = !enabled;
                    if (enabled) {
                        starts.add(connectionLog.getTime());
                    } else {
                        ends.add(connectionLog.getTime());
                    }
                }
            }
            if (starts.size() != ends.size()) {
                ends.add(new Timestamp(System.currentTimeMillis()));
            }

            Timestamp start = date;
            Timestamp end = new Timestamp(date.getTime() + 86400000);
            Long length = 0L;
            for (int i = 0; i < starts.size(); i++) {
                if (starts.get(i).getTime() <= start.getTime() && ends.get(i).getTime() > start.getTime()) {
                    if (ends.get(i).getTime() >= end.getTime()) {
                        length += 86400000;
                    } else {
                        length += ends.get(i).getTime() - start.getTime();
                    }
                } else if (starts.get(i).getTime() > start.getTime() && starts.get(i).getTime() < end.getTime()) {
                    if (ends.get(i).getTime() >= end.getTime()) {
                        length += ends.get(i).getTime() - start.getTime();
                    } else {
                        length += ends.get(i).getTime() - starts.get(i).getTime();
                    }
                }
            }

            lengthMap.put(connection.getId(), length);
        }

        connectionList.sort(
            (Connection connection1, Connection connection2) -> {
                Long length1 = lengthMap.get(connection1.getId());
                Long length2 = lengthMap.get(connection2.getId());
                return length2.compareTo(length1);
            }
        );

        List<Integer> clientPortIdList = new ArrayList<Integer>();
        for (Connection connection : connectionList) {
            if (lengthMap.get(connection.getId()) > 0) {
                clientPortIdList.add(connection.getClientPort().getId());
            }
        }

        return clientPortIdList;
    }

    public List<Integer> getPairedVendorPort(Integer clientPortId, Timestamp date) {
        ClientPort clientPort = clientPortRepository.findById(clientPortId).orElse(null);
        if (clientPort == null) {
            return new ArrayList<Integer>();
        }

        List<Connection> connectionList = new ArrayList<Connection>(connectionRepository.findByClientPort(clientPort).stream().filter(connection -> (connection.getValidFrom().equals(date) || connection.getValidFrom().before(date)) && connection.getValidTo().after(date)).toList());
        List<ConnectionLog> connectionLogList = connectionLogRepository.findByConnectionInOrderByTime(connectionList);
        Map<Integer, Long> lengthMap = new HashMap<Integer, Long>();

        for (Connection connection : connectionList) {
            List<Timestamp> starts = new ArrayList<Timestamp>();
            List<Timestamp> ends = new ArrayList<Timestamp>();
            Boolean enabled = false;
            List<ConnectionLog> connectionLogList1 = connectionLogList.stream().filter(connectionLog -> connectionLog.getConnection().getId().equals(connection.getId())).toList();
            for (ConnectionLog connectionLog : connectionLogList1) {
                if (connectionLog.getEnabled() != enabled) {
                    enabled = !enabled;
                    if (enabled) {
                        starts.add(connectionLog.getTime());
                    } else {
                        ends.add(connectionLog.getTime());
                    }
                }
            }
            if (starts.size() != ends.size()) {
                ends.add(new Timestamp(System.currentTimeMillis()));
            }

            Timestamp start = date;
            Timestamp end = new Timestamp(date.getTime() + 86400000);
            Long length = 0L;
            for (int i = 0; i < starts.size(); i++) {
                if (starts.get(i).getTime() <= start.getTime() && ends.get(i).getTime() > start.getTime()) {
                    if (ends.get(i).getTime() >= end.getTime()) {
                        length += 86400000;
                    } else {
                        length += ends.get(i).getTime() - start.getTime();
                    }
                } else if (starts.get(i).getTime() > start.getTime() && starts.get(i).getTime() < end.getTime()) {
                    if (ends.get(i).getTime() >= end.getTime()) {
                        length += ends.get(i).getTime() - start.getTime();
                    } else {
                        length += ends.get(i).getTime() - starts.get(i).getTime();
                    }
                }
            }

            lengthMap.put(connection.getId(), length);
        }

        connectionList.sort(
            (Connection connection1, Connection connection2) -> {
                Long length1 = lengthMap.get(connection1.getId());
                Long length2 = lengthMap.get(connection2.getId());
                return length2.compareTo(length1);
            }
        );

        List<Integer> vendorPortIdList = new ArrayList<Integer>();
        for (Connection connection : connectionList) {
            if (lengthMap.get(connection.getId()) > 0) {
                vendorPortIdList.add(connection.getVendorPort().getId());
            }
        }

        return vendorPortIdList;
    }

    public Map<Integer, List<Integer>> getPairedClientPortMap(List<Integer> vendorPortIdList, Timestamp date) {
        List<VendorPort> vendorPortList = vendorPortRepository.findAll();
        List<Connection> fullConnectionList = connectionRepository.findByVendorPortIn(vendorPortList);
        List<ConnectionLog> fullConnectionLogList = connectionLogRepository.findByConnectionInOrderByTime(fullConnectionList);

        Map<Integer, List<Integer>> pairedClientPortIdMap = new HashMap<Integer, List<Integer>>();

        for (Integer vendorPortId : vendorPortIdList) {
            if (vendorPortList.stream().filter(vendorPort -> vendorPort.getId().equals(vendorPortId)).findFirst().orElse(null) == null) {
                continue;
            }

            List<Connection> connectionList = new ArrayList<Connection>(fullConnectionList.stream().filter(connection -> connection.getVendorPort().getId().equals(vendorPortId) && (connection.getValidFrom().equals(new Timestamp(date.getTime() + 86400000)) || connection.getValidFrom().before(new Timestamp(date.getTime() + 86400000))) && connection.getValidTo().after(date)).toList());
            List<ConnectionLog> connectionLogList = fullConnectionLogList.stream().filter(connectionLog -> connectionList.stream().map(Connection::getId).toList().contains(connectionLog.getConnection().getId())).toList();
            Map<Integer, Long> lengthMap = new HashMap<Integer, Long>();

            for (Connection connection : connectionList) {
                List<Timestamp> starts = new ArrayList<Timestamp>();
                List<Timestamp> ends = new ArrayList<Timestamp>();
                Boolean enabled = false;
                List<ConnectionLog> connectionLogList1 = connectionLogList.stream().filter(connectionLog -> connectionLog.getConnection().getId().equals(connection.getId())).toList();
                for (ConnectionLog connectionLog : connectionLogList1) {
                    if (connectionLog.getEnabled() != enabled) {
                        enabled = !enabled;
                        if (enabled) {
                            starts.add(connectionLog.getTime());
                        } else {
                            ends.add(connectionLog.getTime());
                        }
                    }
                }
                if (starts.size() != ends.size()) {
                    ends.add(new Timestamp(System.currentTimeMillis()));
                }

                Timestamp start = date;
                Timestamp end = new Timestamp(date.getTime() + 86400000);
                Long length = 0L;
                for (int i = 0; i < starts.size(); i++) {
                    if (starts.get(i).getTime() <= start.getTime() && ends.get(i).getTime() > start.getTime()) {
                        if (ends.get(i).getTime() >= end.getTime()) {
                            length += 86400000;
                        } else {
                            length += ends.get(i).getTime() - start.getTime();
                        }
                    } else if (starts.get(i).getTime() > start.getTime() && starts.get(i).getTime() < end.getTime()) {
                        if (ends.get(i).getTime() >= end.getTime()) {
                            length += ends.get(i).getTime() - start.getTime();
                        } else {
                            length += ends.get(i).getTime() - starts.get(i).getTime();
                        }
                    }
                }

                lengthMap.put(connection.getId(), length);
            }

            connectionList.sort(
                (Connection connection1, Connection connection2) -> {
                    Long length1 = lengthMap.get(connection1.getId());
                    Long length2 = lengthMap.get(connection2.getId());
                    return length2.compareTo(length1);
                }
            );

            List<Integer> clientPortIdList = new ArrayList<Integer>();
            for (Connection connection : connectionList) {
                if (lengthMap.get(connection.getId()) > 0) {
                    clientPortIdList.add(connection.getClientPort().getId());
                }
            }

            pairedClientPortIdMap.put(vendorPortId, clientPortIdList);
        }

        return pairedClientPortIdMap;
    }

    public Map<Integer, List<Integer>> getPairedVendorPortMap(List<Integer> clientPortIdList, Timestamp date) {
        List<ClientPort> clientPortList = clientPortRepository.findAll();
        List<Connection> fullConnectionList = connectionRepository.findByClientPortIn(clientPortList);
        List<ConnectionLog> fullConnectionLogList = connectionLogRepository.findByConnectionInOrderByTime(fullConnectionList);

        Map<Integer, List<Integer>> pairedVendorPortIdMap = new HashMap<Integer, List<Integer>>();

        for (Integer clientPortId : clientPortIdList) {
            if (clientPortList.stream().filter(clientPort -> clientPort.getId().equals(clientPortId)).findFirst().orElse(null) == null) {
                continue;
            }

            List<Connection> connectionList = new ArrayList<Connection>(fullConnectionList.stream().filter(connection -> connection.getClientPort().getId().equals(clientPortId) && (connection.getValidFrom().equals(new Timestamp(date.getTime() + 86400000)) || connection.getValidFrom().before(new Timestamp(date.getTime() + 86400000))) && connection.getValidTo().after(date)).toList());
            List<ConnectionLog> connectionLogList = fullConnectionLogList.stream().filter(connectionLog -> connectionList.stream().map(Connection::getId).toList().contains(connectionLog.getConnection().getId())).toList();
            Map<Integer, Long> lengthMap = new HashMap<Integer, Long>();

            for (Connection connection : connectionList) {
                List<Timestamp> starts = new ArrayList<Timestamp>();
                List<Timestamp> ends = new ArrayList<Timestamp>();
                Boolean enabled = false;
                List<ConnectionLog> connectionLogList1 = connectionLogList.stream().filter(connectionLog -> connectionLog.getConnection().getId().equals(connection.getId())).toList();
                for (ConnectionLog connectionLog : connectionLogList1) {
                    if (connectionLog.getEnabled() != enabled) {
                        enabled = !enabled;
                        if (enabled) {
                            starts.add(connectionLog.getTime());
                        } else {
                            ends.add(connectionLog.getTime());
                        }
                    }
                }
                if (starts.size() != ends.size()) {
                    ends.add(new Timestamp(System.currentTimeMillis()));
                }

                Timestamp start = date;
                Timestamp end = new Timestamp(date.getTime() + 86400000);
                Long length = 0L;
                for (int i = 0; i < starts.size(); i++) {
                    if (starts.get(i).getTime() <= start.getTime() && ends.get(i).getTime() > start.getTime()) {
                        if (ends.get(i).getTime() >= end.getTime()) {
                            length += 86400000;
                        } else {
                            length += ends.get(i).getTime() - start.getTime();
                        }
                    } else if (starts.get(i).getTime() > start.getTime() && starts.get(i).getTime() < end.getTime()) {
                        if (ends.get(i).getTime() >= end.getTime()) {
                            length += ends.get(i).getTime() - start.getTime();
                        } else {
                            length += ends.get(i).getTime() - starts.get(i).getTime();
                        }
                    }
                }

                lengthMap.put(connection.getId(), length);
            }

            connectionList.sort(
                (Connection connection1, Connection connection2) -> {
                    Long length1 = lengthMap.get(connection1.getId());
                    Long length2 = lengthMap.get(connection2.getId());
                    return length2.compareTo(length1);
                }
            );

            List<Integer> vendorPortIdList = new ArrayList<Integer>();
            for (Connection connection : connectionList) {
                if (lengthMap.get(connection.getId()) > 0) {
                    vendorPortIdList.add(connection.getVendorPort().getId());
                }
            }

            pairedVendorPortIdMap.put(clientPortId, vendorPortIdList);
        }

        return pairedVendorPortIdMap;
    }

}
