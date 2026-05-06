package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.ClientPortRepository;
import cc.tonyhook.carambola.backend.dao.ad.ConnectionLogRepository;
import cc.tonyhook.carambola.backend.dao.ad.ConnectionRepository;
import cc.tonyhook.carambola.backend.dao.ad.VendorPortRepository;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.ConnectionLog;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class ConnectionService {

    private final ClientPortRepository clientPortRepository;
    private final VendorPortRepository vendorPortRepository;
    private final ConnectionRepository connectionRepository;
    private final ConnectionLogRepository connectionLogRepository;
    private final PartnerService partnerService;

    public ConnectionService(
            ClientPortRepository clientPortRepository,
            VendorPortRepository vendorPortRepository,
            ConnectionRepository connectionRepository,
            ConnectionLogRepository connectionLogRepository,
            PartnerService partnerService
    ) {
        this.clientPortRepository = clientPortRepository;
        this.vendorPortRepository = vendorPortRepository;
        this.connectionRepository = connectionRepository;
        this.connectionLogRepository = connectionLogRepository;
        this.partnerService = partnerService;
    }

    public List<Connection> getConnectionList() {
        List<Connection> connectionList = connectionRepository.findAll();

        return connectionList;
    }

    public Connection getConnection(Integer id) {
        Connection connection = connectionRepository.findById(id).orElse(null);

        return connection;
    }

    @Transactional
    public Connection addConnection(Connection newConnection) {
        Connection updatedConnection = connectionRepository.save(newConnection);
        touchConnectionPorts(updatedConnection);

        return updatedConnection;
    }

    @Transactional
    public void updateConnection(Integer id, Connection newConnection) {
        Connection targetConnection = connectionRepository.findById(id).orElse(null);
        Connection updatedConnection = connectionRepository.save(newConnection);
        touchConnectionPorts(targetConnection);
        touchConnectionPorts(updatedConnection);
    }

    @Transactional
    public void removeConnection(Integer id) {
        Connection deletedConnection = connectionRepository.findById(id).orElse(null);

        if (deletedConnection != null) {
            deletedConnection.setDeleted(true);
            touchConnectionPorts(deletedConnection);
        }
    }

    private void touchConnectionPorts(Connection connection) {
        if (connection == null) {
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (connection.getClientPort() != null && connection.getClientPort().getId() != null) {
            ClientPort clientPort = clientPortRepository.findById(connection.getClientPort().getId()).orElse(null);
            if (clientPort != null) {
                clientPort.setUpdateTime(now);
            }
        }
        if (connection.getVendorPort() != null && connection.getVendorPort().getId() != null) {
            VendorPort vendorPort = vendorPortRepository.findById(connection.getVendorPort().getId()).orElse(null);
            if (vendorPort != null) {
                vendorPort.setUpdateTime(now);
            }
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

    public Map<Long, Map<Integer, List<Integer>>> getPairedClientPortMap(List<Integer> vendorPortIdList, Timestamp startDate, Timestamp endDate) {
        List<VendorPort> vendorPortList = vendorPortRepository.findAll();
        List<Connection> fullConnectionList = connectionRepository.findByVendorPortIn(vendorPortList);
        List<ConnectionLog> fullConnectionLogList = connectionLogRepository.findByConnectionInOrderByTime(fullConnectionList);

        Map<Long, Map<Integer, List<Integer>>> pairedClientPortIdMap = new HashMap<Long, Map<Integer, List<Integer>>>();

        Timestamp date = new Timestamp(startDate.getTime());
        while (date.before(endDate) || date.equals(endDate)) {
            final Timestamp finalDate = date;
            Map<Integer, List<Integer>> pairedClientPortIdMapOneDay = new HashMap<Integer, List<Integer>>();
            pairedClientPortIdMap.put(finalDate.getTime(), pairedClientPortIdMapOneDay);

            for (Integer vendorPortId : vendorPortIdList) {
                if (vendorPortList.stream().filter(vendorPort -> vendorPort.getId().equals(vendorPortId)).findFirst().orElse(null) == null) {
                    continue;
                }

                List<Connection> connectionList = new ArrayList<Connection>(fullConnectionList.stream().filter(connection -> connection.getVendorPort().getId().equals(vendorPortId) && (connection.getValidFrom().equals(new Timestamp(finalDate.getTime() + 86400000)) || connection.getValidFrom().before(new Timestamp(finalDate.getTime() + 86400000))) && connection.getValidTo().after(finalDate)).toList());
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

                    Timestamp start = finalDate;
                    Timestamp end = new Timestamp(finalDate.getTime() + 86400000);
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

                if (clientPortIdList.size() >= 0) {
                    pairedClientPortIdMapOneDay.put(vendorPortId, clientPortIdList);
                }
            }

            date = new Timestamp(date.getTime() + 86400000);
        }

        return pairedClientPortIdMap;
    }

    public Map<Long, Map<Integer, List<Integer>>> getPairedVendorPortMap(List<Integer> clientPortIdList, Timestamp startDate, Timestamp endDate) {
        List<ClientPort> clientPortList = clientPortRepository.findAll();
        List<Connection> fullConnectionList = connectionRepository.findByClientPortIn(clientPortList);
        List<ConnectionLog> fullConnectionLogList = connectionLogRepository.findByConnectionInOrderByTime(fullConnectionList);

        Map<Long, Map<Integer, List<Integer>>> pairedVendorPortIdMap = new HashMap<Long, Map<Integer, List<Integer>>>();

        Timestamp date = new Timestamp(startDate.getTime());
        while (date.before(endDate) || date.equals(endDate)) {
            final Timestamp finalDate = date;
            Map<Integer, List<Integer>> pairedVendorPortIdMapOneDay = new HashMap<Integer, List<Integer>>();
            pairedVendorPortIdMap.put(finalDate.getTime(), pairedVendorPortIdMapOneDay);

            for (Integer clientPortId : clientPortIdList) {
                if (clientPortList.stream().filter(clientPort -> clientPort.getId().equals(clientPortId)).findFirst().orElse(null) == null) {
                    continue;
                }

                List<Connection> connectionList = new ArrayList<Connection>(fullConnectionList.stream().filter(connection -> connection.getClientPort().getId().equals(clientPortId) && (connection.getValidFrom().equals(new Timestamp(finalDate.getTime() + 86400000)) || connection.getValidFrom().before(new Timestamp(finalDate.getTime() + 86400000))) && connection.getValidTo().after(finalDate)).toList());
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

                    Timestamp start = finalDate;
                    Timestamp end = new Timestamp(finalDate.getTime() + 86400000);
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

                if (vendorPortIdList.size() >= 0) {
                    pairedVendorPortIdMapOneDay.put(clientPortId, vendorPortIdList);
                }
            }

            date = new Timestamp(date.getTime() + 86400000);
        }

        return pairedVendorPortIdMap;
    }

    public Map<Long, Map<Integer, List<Integer>>> queryPairedClientPortMap(
            Authentication authentication,
            Query query,
            Timestamp startDate,
            Timestamp endDate,
            String timezone) {
        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        Set<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toSet());
        List<Connection> fullConnectionList = connectionRepository.findByVendorPortIn(vendorPortList);
        List<ConnectionLog> fullConnectionLogList = connectionLogRepository.findByConnectionInOrderByTime(fullConnectionList);

        Map<Long, Map<Integer, List<Integer>>> pairedClientPortIdMap = new HashMap<Long, Map<Integer, List<Integer>>>();

        TimeZone tz = TimeZone.getTimeZone(timezone);
        Calendar calendar = Calendar.getInstance(tz);
        calendar.setTimeInMillis(startDate.getTime());
        Timestamp date = new Timestamp(calendar.getTime().getTime());
        while (date.before(endDate) || date.equals(endDate)) {
            final Timestamp finalDate = date;
            Map<Integer, List<Integer>> pairedClientPortIdMapOneDay = new HashMap<Integer, List<Integer>>();
            pairedClientPortIdMap.put(finalDate.getTime(), pairedClientPortIdMapOneDay);

            for (Integer vendorPortId : vendorPortIdList) {
                if (vendorPortList.stream().filter(vendorPort -> vendorPort.getId().equals(vendorPortId)).findFirst().orElse(null) == null) {
                    continue;
                }

                List<Connection> connectionList = new ArrayList<Connection>(fullConnectionList.stream().filter(connection -> connection.getVendorPort().getId().equals(vendorPortId) && (connection.getValidFrom().equals(new Timestamp(finalDate.getTime() + 86400000)) || connection.getValidFrom().before(new Timestamp(finalDate.getTime() + 86400000))) && connection.getValidTo().after(finalDate)).toList());
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

                    Timestamp start = finalDate;
                    Timestamp end = new Timestamp(finalDate.getTime() + 86400000);
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

                if (clientPortIdList.size() >= 0) {
                    pairedClientPortIdMapOneDay.put(vendorPortId, clientPortIdList);
                }
            }

            calendar.setTimeInMillis(date.getTime());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            date.setTime(calendar.getTime().getTime());
        }

        return pairedClientPortIdMap;
    }

    public Map<Long, Map<Integer, List<Integer>>> queryPairedVendorPortMap(
            Authentication authentication,
            Query query,
            Timestamp startDate,
            Timestamp endDate,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        Set<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toSet());
        List<Connection> fullConnectionList = connectionRepository.findByClientPortIn(clientPortList);
        List<ConnectionLog> fullConnectionLogList = connectionLogRepository.findByConnectionInOrderByTime(fullConnectionList);

        Map<Long, Map<Integer, List<Integer>>> pairedVendorPortIdMap = new HashMap<Long, Map<Integer, List<Integer>>>();

        TimeZone tz = TimeZone.getTimeZone(timezone);
        Calendar calendar = Calendar.getInstance(tz);
        calendar.setTimeInMillis(startDate.getTime());
        Timestamp date = new Timestamp(startDate.getTime());
        while (date.before(endDate) || date.equals(endDate)) {
            final Timestamp finalDate = date;
            Map<Integer, List<Integer>> pairedVendorPortIdMapOneDay = new HashMap<Integer, List<Integer>>();
            pairedVendorPortIdMap.put(finalDate.getTime(), pairedVendorPortIdMapOneDay);

            for (Integer clientPortId : clientPortIdList) {
                if (clientPortList.stream().filter(clientPort -> clientPort.getId().equals(clientPortId)).findFirst().orElse(null) == null) {
                    continue;
                }

                List<Connection> connectionList = new ArrayList<Connection>(fullConnectionList.stream().filter(connection -> connection.getClientPort().getId().equals(clientPortId) && (connection.getValidFrom().equals(new Timestamp(finalDate.getTime() + 86400000)) || connection.getValidFrom().before(new Timestamp(finalDate.getTime() + 86400000))) && connection.getValidTo().after(finalDate)).toList());
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

                    Timestamp start = finalDate;
                    Timestamp end = new Timestamp(finalDate.getTime() + 86400000);
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

                if (vendorPortIdList.size() >= 0) {
                    pairedVendorPortIdMapOneDay.put(clientPortId, vendorPortIdList);
                }
            }

            calendar.setTimeInMillis(date.getTime());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.DAY_OF_MONTH, 1);

            date.setTime(calendar.getTime().getTime());
        }

        return pairedVendorPortIdMap;
    }

}
