package cc.tonyhook.carambola.backend.service.scheduled;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientBundleDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientBundleHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientBundleQuarterRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientQuarterRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorBundleDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorBundleHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorBundleQuarterRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorQuarterRepository;
import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.Finance;
import cc.tonyhook.carambola.backend.entity.ad.FinanceBundle;
import cc.tonyhook.carambola.backend.entity.ad.Performance;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceBundle;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleQuarter;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientQuarter;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundleDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundleHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundleQuarter;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorQuarter;
import cc.tonyhook.carambola.backend.entity.ad.Server;
import cc.tonyhook.carambola.backend.service.ad.ConnectionService;
import cc.tonyhook.carambola.backend.service.ad.PerformanceService;
import cc.tonyhook.carambola.backend.service.ad.ServerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Component
@Configuration
public class PerformanceCollectingService {

    private final PerformanceClientQuarterRepository performanceClientQuarterRepository;
    private final PerformanceClientHourRepository performanceClientHourRepository;
    private final PerformanceClientDayRepository performanceClientDayRepository;
    private final PerformanceVendorQuarterRepository performanceVendorQuarterRepository;
    private final PerformanceVendorHourRepository performanceVendorHourRepository;
    private final PerformanceVendorDayRepository performanceVendorDayRepository;
    private final PerformanceClientBundleQuarterRepository performanceClientBundleQuarterRepository;
    private final PerformanceClientBundleHourRepository performanceClientBundleHourRepository;
    private final PerformanceClientBundleDayRepository performanceClientBundleDayRepository;
    private final PerformanceVendorBundleQuarterRepository performanceVendorBundleQuarterRepository;
    private final PerformanceVendorBundleHourRepository performanceVendorBundleHourRepository;
    private final PerformanceVendorBundleDayRepository performanceVendorBundleDayRepository;

    private final ConnectionService connectionService;
    private final PerformanceService performanceService;
    private final ServerService serverService;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.performance-interval:5}")
    private Integer performanceInterval;

    private Map<String, JedisConnectionFactory> jedisConnectionFactoryMap = new HashMap<String, JedisConnectionFactory>();

    public PerformanceCollectingService(
            PerformanceClientQuarterRepository performanceClientQuarterRepository,
            PerformanceClientHourRepository performanceClientHourRepository,
            PerformanceClientDayRepository performanceClientDayRepository,
            PerformanceVendorQuarterRepository performanceVendorQuarterRepository,
            PerformanceVendorHourRepository performanceVendorHourRepository,
            PerformanceVendorDayRepository performanceVendorDayRepository,
            PerformanceClientBundleQuarterRepository performanceClientBundleQuarterRepository,
            PerformanceClientBundleHourRepository performanceClientBundleHourRepository,
            PerformanceClientBundleDayRepository performanceClientBundleDayRepository,
            PerformanceVendorBundleQuarterRepository performanceVendorBundleQuarterRepository,
            PerformanceVendorBundleHourRepository performanceVendorBundleHourRepository,
            PerformanceVendorBundleDayRepository performanceVendorBundleDayRepository,
            ConnectionService connectionService,
            PerformanceService performanceService,
            ServerService serverService,
            TransactionTemplate transactionTemplate
    ) {
        this.performanceClientQuarterRepository = performanceClientQuarterRepository;
        this.performanceClientHourRepository = performanceClientHourRepository;
        this.performanceClientDayRepository = performanceClientDayRepository;
        this.performanceVendorQuarterRepository = performanceVendorQuarterRepository;
        this.performanceVendorHourRepository = performanceVendorHourRepository;
        this.performanceVendorDayRepository = performanceVendorDayRepository;
        this.performanceClientBundleQuarterRepository = performanceClientBundleQuarterRepository;
        this.performanceClientBundleHourRepository = performanceClientBundleHourRepository;
        this.performanceClientBundleDayRepository = performanceClientBundleDayRepository;
        this.performanceVendorBundleQuarterRepository = performanceVendorBundleQuarterRepository;
        this.performanceVendorBundleHourRepository = performanceVendorBundleHourRepository;
        this.performanceVendorBundleDayRepository = performanceVendorBundleDayRepository;
        this.connectionService = connectionService;
        this.performanceService = performanceService;
        this.serverService = serverService;
        this.transactionTemplate = transactionTemplate;
    }

    public JedisConnectionFactory jedisConnectionFactory(String host, Integer port) {
        if (jedisConnectionFactoryMap.containsKey(host + ":" + port)) {
            return jedisConnectionFactoryMap.get(host + ":" + port);
        }

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder().readTimeout(Duration.ofMillis(60000)).build();
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);
        jedisConnectionFactory.start();

        jedisConnectionFactoryMap.put(host + ":" + port, jedisConnectionFactory);

        return jedisConnectionFactory;
    }

    public RedisTemplate<String, Integer> redisTemplate(String host, Integer port) {
        RedisTemplate<String, Integer> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory(host, port));

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        GenericToStringSerializer<Integer> genericToStringSerializer = new GenericToStringSerializer<Integer>(Integer.class);
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(genericToStringSerializer);
        template.afterPropertiesSet();

        return template;
    }

    // Note:
    // collecting interval should equal or larger than performance interval
    // postpone should less than collecting interval
    @Scheduled(cron = "30 0/15 * * * ?")
    public void collectPerformance() {
        List<Connection> connectionList = connectionService.getConnectionList();
        Map<Integer, Connection> connectionMap = connectionList.stream().collect(Collectors.toMap(Connection::getId, Function.identity()));

        Calendar base_calendar = Calendar.getInstance();
        int base_hour = base_calendar.get(Calendar.HOUR_OF_DAY);
        int base_minute = base_calendar.get(Calendar.MINUTE);
        base_minute = base_minute / performanceInterval * performanceInterval;
        base_calendar.set(Calendar.MINUTE, base_minute);
        base_calendar.set(Calendar.SECOND, 0);
        base_calendar.set(Calendar.MILLISECOND, 0);

        List<Performance> performanceList = new ArrayList<Performance>();
        Map<String, Performance> performanceMap = new HashMap<String, Performance>();
        List<PerformanceBundle> performanceBundleList = new ArrayList<PerformanceBundle>();
        Map<String, PerformanceBundle> performanceBundleMap = new HashMap<String, PerformanceBundle>();
        List<Finance> financeList = new ArrayList<Finance>();
        Map<String, Finance> financeMap = new HashMap<String, Finance>();
        List<FinanceBundle> financeBundleList = new ArrayList<FinanceBundle>();
        Map<String, FinanceBundle> financeBundleMap = new HashMap<String, FinanceBundle>();

        List<Server> serverList = serverService.getServerList(null);
        Timestamp from_performance = null;
        Timestamp to_performance = null;
        Timestamp from_finance = null;
        Timestamp to_finance = null;

        for (Server server : serverList) {
            try {
                String host = server.getAddress();
                Integer port = 6480;
                Integer node = server.getNode();

                RedisTemplate<String, Integer> redisTemplate = redisTemplate(host, port);

                Set<String> keys_performance = redisTemplate.keys("P*");
                for (String key : keys_performance) {
                    int hour = Integer.parseInt(key.substring(1, 3));
                    int minute = Integer.parseInt(key.substring(3, 5));

                    if (hour == base_hour && minute == base_minute) {
                        continue;
                    }

                    Calendar performance_calendar = (Calendar) base_calendar.clone();
                    performance_calendar.set(Calendar.HOUR_OF_DAY, hour);
                    performance_calendar.set(Calendar.MINUTE, minute);
                    if (hour > base_hour || (hour == base_hour && minute > base_minute)) {
                        // last day
                        performance_calendar.add(Calendar.DATE, -1);
                    }

                    if (from_performance == null || from_performance.after(new Timestamp(performance_calendar.getTimeInMillis()))) {
                        from_performance = new Timestamp(performance_calendar.getTimeInMillis());
                    }
                    if (to_performance == null || to_performance.before(new Timestamp(performance_calendar.getTimeInMillis()))) {
                        to_performance = new Timestamp(performance_calendar.getTimeInMillis());
                    }
                }

                Set<String> keys_tracking = redisTemplate.keys("T1*");
                for (String key : keys_tracking) {
                    int hour = Integer.parseInt(key.substring(2, 4));
                    int minute = Integer.parseInt(key.substring(4, 6));
                    minute = minute / performanceInterval * performanceInterval;

                    if (hour == base_hour && minute == base_minute) {
                        continue;
                    }

                    Calendar tracking_calendar = (Calendar) base_calendar.clone();
                    tracking_calendar.set(Calendar.HOUR_OF_DAY, hour);
                    tracking_calendar.set(Calendar.MINUTE, minute);
                    if (hour > base_hour || (hour == base_hour && minute > base_minute)) {
                        // last day
                        tracking_calendar.add(Calendar.DATE, -1);
                    }

                    if (from_performance == null || from_performance.after(new Timestamp(tracking_calendar.getTimeInMillis()))) {
                        from_performance = new Timestamp(tracking_calendar.getTimeInMillis());
                    }
                    if (to_performance == null || to_performance.before(new Timestamp(tracking_calendar.getTimeInMillis()))) {
                        to_performance = new Timestamp(tracking_calendar.getTimeInMillis());
                    }
                }

                List<PerformanceBundle> existedPerformanceBundleList = performanceService.getPerformanceBundleList(from_performance, to_performance);
                Map<String, PerformanceBundle> existedPerformanceBundleMap = new HashMap<String, PerformanceBundle>();
                for (PerformanceBundle performanceBundle : existedPerformanceBundleList) {
                    Calendar performance_calendar = Calendar.getInstance();
                    performance_calendar.setTimeInMillis(performanceBundle.getTime().getTime());
                    String key = performance_calendar.get(Calendar.HOUR_OF_DAY) + "|" + performance_calendar.get(Calendar.MINUTE) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle() + "|" + performanceBundle.getNode() + "|" + performanceBundle.getEvent();
                    existedPerformanceBundleMap.put(key, performanceBundle);
                }

                for (String key : keys_performance) {
                    int hour = Integer.parseInt(key.substring(1, 3));
                    int minute = Integer.parseInt(key.substring(3, 5));

                    if (hour == base_hour && minute == base_minute) {
                        continue;
                    }

                    Calendar performance_calendar = (Calendar) base_calendar.clone();
                    performance_calendar.set(Calendar.HOUR_OF_DAY, hour);
                    performance_calendar.set(Calendar.MINUTE, minute);
                    if (hour > base_hour || (hour == base_hour && minute > base_minute)) {
                        // last day
                        performance_calendar.add(Calendar.DATE, -1);
                    }

                    Integer clientPort = Integer.parseInt(key.split(":")[1]);
                    Integer vendorPort = Integer.parseInt(key.split(":")[2]);
                    String bundle = "UNKNOWN";
                    String event = null;
                    if (key.split(":").length == 5) {
                        bundle = key.split(":")[3];
                        event = key.split(":")[4];
                    } else {
                        event = key.split(":")[3];
                    }

                    if (existedPerformanceBundleMap.containsKey(hour + "|" + minute + "|" + clientPort + "|" + vendorPort + "|" + bundle + "|" +  node + "|" + event)) {
                        continue;
                    }

                    Integer value = redisTemplate.opsForValue().get(key);

                    String performanceKey = hour + "|" + minute + "|" + clientPort + "|" + vendorPort + "|" + node + "|" + event;
                    if (!performanceMap.containsKey(performanceKey)) {
                        Performance performance = new Performance(clientPort, vendorPort, node, new Timestamp(performance_calendar.getTimeInMillis()), event);

                        performanceList.add(performance);
                        performanceMap.put(performanceKey, performance);
                    }
                    Performance performance = performanceMap.get(performanceKey);
                    performance.setAmount(performance.getAmount() + value);

                    String performanceBundleKey = hour + "|" + minute + "|" + clientPort + "|" + vendorPort + "|" + bundle + "|" + node + "|" + event;
                    if (!performanceBundleMap.containsKey(performanceBundleKey)) {
                        PerformanceBundle performanceBundle = new PerformanceBundle(clientPort, vendorPort, bundle, node, new Timestamp(performance_calendar.getTimeInMillis()), event);

                        performanceBundleList.add(performanceBundle);
                        performanceBundleMap.put(performanceBundleKey, performanceBundle);
                    }
                    PerformanceBundle performanceBundle = performanceBundleMap.get(performanceBundleKey);
                    performanceBundle.setAmount(performanceBundle.getAmount() + value);
                }

                for (String key : keys_tracking) {
                    int hour = Integer.parseInt(key.substring(2, 4));
                    int minute = Integer.parseInt(key.substring(4, 6));
                    minute = minute / performanceInterval * performanceInterval;

                    if (hour == base_hour && minute == base_minute) {
                        continue;
                    }

                    Calendar tracking_calendar = (Calendar) base_calendar.clone();
                    tracking_calendar.set(Calendar.HOUR_OF_DAY, hour);
                    tracking_calendar.set(Calendar.MINUTE, minute);
                    if (hour > base_hour || (hour == base_hour && minute > base_minute)) {
                        // last day
                        tracking_calendar.add(Calendar.DATE, -1);
                    }

                    Integer connection = Integer.parseInt(key.split(":")[1]);
                    String bundle = "UNKNOWN";
                    String event = "";
                    Integer eventCode = 0;
                    if (key.split(":").length == 4) {
                        bundle = key.split(":")[2];
                        eventCode = Integer.parseInt(key.split(":")[3]);
                    } else {
                        eventCode = Integer.parseInt(key.split(":")[2]);
                    }
                    switch (eventCode) {
                        case 501:
                            event = Performance.TRACKING_IMPRESSION;
                            break;
                        case 502:
                            event = Performance.TRACKING_CLICK;
                            break;
                        default:
                            event = Performance.TRACKING_GENERAL;
                            break;
                    }

                    if (connectionMap.containsKey(connection) && existedPerformanceBundleMap.containsKey(hour + "|" + minute + "|" + connectionMap.get(connection).getClientPort().getId() + "|" + connectionMap.get(connection).getVendorPort().getId() + "|" + bundle + "|" +  node + "|" + event)) {
                        continue;
                    }

                    Integer value = redisTemplate.opsForValue().get(key);

                    if (connectionMap.containsKey(connection)) {
                        String performanceKey = hour + "|" + minute + "|" + connectionMap.get(connection).getClientPort().getId() + "|" + connectionMap.get(connection).getVendorPort().getId() + "|" + node + "|" + event;
                        if (!performanceMap.containsKey(performanceKey)) {
                            Performance performance = new Performance(connectionMap.get(connection).getClientPort().getId(), connectionMap.get(connection).getVendorPort().getId(), node, new Timestamp(tracking_calendar.getTimeInMillis()), event);

                            performanceList.add(performance);
                            performanceMap.put(performanceKey, performance);
                        }
                        Performance performance = performanceMap.get(performanceKey);
                        performance.setAmount(performance.getAmount() + value);

                        String performanceBundleKey = hour + "|" + minute + "|" + connectionMap.get(connection).getClientPort().getId() + "|" + connectionMap.get(connection).getVendorPort().getId() + "|" + bundle + "|" + node + "|" + event;
                        if (!performanceBundleMap.containsKey(performanceBundleKey)) {
                            PerformanceBundle performanceBundle = new PerformanceBundle(connectionMap.get(connection).getClientPort().getId(), connectionMap.get(connection).getVendorPort().getId(), bundle, node, new Timestamp(tracking_calendar.getTimeInMillis()), event);

                            performanceBundleList.add(performanceBundle);
                            performanceBundleMap.put(performanceBundleKey, performanceBundle);
                        }
                        PerformanceBundle performanceBundle = performanceBundleMap.get(performanceBundleKey);
                        performanceBundle.setAmount(performanceBundle.getAmount() + value);
                    }
                }

                Set<String> keys_finance = redisTemplate.keys("C*");
                for (String key : keys_finance) {
                    int hour = Integer.parseInt(key.substring(2, 4));
                    int minute = Integer.parseInt(key.substring(4, 6));

                    if (hour == base_hour && minute == base_minute) {
                        continue;
                    }

                    Calendar finance_calendar = (Calendar) base_calendar.clone();
                    finance_calendar.set(Calendar.HOUR_OF_DAY, hour);
                    finance_calendar.set(Calendar.MINUTE, minute);
                    if (hour > base_hour || (hour == base_hour && minute > base_minute)) {
                        // last day
                        finance_calendar.add(Calendar.DATE, -1);
                    }

                    if (from_finance == null || from_finance.after(new Timestamp(finance_calendar.getTimeInMillis()))) {
                        from_finance = new Timestamp(finance_calendar.getTimeInMillis());
                    }
                    if (to_finance == null || to_finance.before(new Timestamp(finance_calendar.getTimeInMillis()))) {
                        to_finance = new Timestamp(finance_calendar.getTimeInMillis());
                    }
                }

                List<FinanceBundle> existedFinanceBundleList = performanceService.getFinanceBundleList(from_finance, to_finance);
                Map<String, FinanceBundle> existedFinanceBundleMap = new HashMap<String, FinanceBundle>();
                for (FinanceBundle financeBundle : existedFinanceBundleList) {
                    Calendar finance_calendar = Calendar.getInstance();
                    finance_calendar.setTimeInMillis(financeBundle.getTime().getTime());
                    String key = finance_calendar.get(Calendar.HOUR_OF_DAY) + "|" + finance_calendar.get(Calendar.MINUTE) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle() + "|" + financeBundle.getNode() + "|";
                    if (financeBundle.getIncome() != 0) {
                        existedFinanceBundleMap.put(key + "I", financeBundle);
                    }
                    if (financeBundle.getOutcomeUpstream() != 0) {
                        existedFinanceBundleMap.put(key + "U", financeBundle);
                    }
                    if (financeBundle.getOutcomeRebate() != 0) {
                        existedFinanceBundleMap.put(key + "R", financeBundle);
                    }
                    if (financeBundle.getOutcomeDownstream() != 0) {
                        existedFinanceBundleMap.put(key + "D", financeBundle);
                    }
                }

                for (String key : keys_finance) {
                    int hour = Integer.parseInt(key.substring(2, 4));
                    int minute = Integer.parseInt(key.substring(4, 6));

                    if (hour == base_hour && minute == base_minute) {
                        continue;
                    }

                    Calendar finance_calendar = (Calendar) base_calendar.clone();
                    finance_calendar.set(Calendar.HOUR_OF_DAY, hour);
                    finance_calendar.set(Calendar.MINUTE, minute);
                    if (hour > base_hour || (hour == base_hour && minute > base_minute)) {
                        // last day
                        finance_calendar.add(Calendar.DATE, -1);
                    }

                    Integer clientPort = Integer.parseInt(key.split(":")[1]);
                    Integer vendorPort = Integer.parseInt(key.split(":")[2]);
                    String type = key.substring(1, 2);
                    String bundle = "UNKNOWN";
                    if (key.split(":").length == 4) {
                        bundle = key.split(":")[3];
                    }

                    if (existedFinanceBundleMap.containsKey(hour + "|" + minute + "|" + clientPort + "|" + vendorPort + "|" + bundle + "|" + node + "|" + type)) {
                        continue;
                    }

                    Long value = Double.valueOf(redisTemplate.opsForValue().get(key)).longValue();

                    String financeKey = hour + "|" + minute + "|" + clientPort + "|" + vendorPort + "|" + node;
                    if (!financeMap.containsKey(financeKey)) {
                        Finance finance = new Finance(clientPort, vendorPort, node, new Timestamp(finance_calendar.getTimeInMillis()));

                        financeList.add(finance);
                        financeMap.put(financeKey, finance);
                    }
                    Finance finance = financeMap.get(financeKey);
                    if (type.equals("I")) {
                        finance.setIncome(finance.getIncome() + value);
                    }
                    if (type.equals("U")) {
                        finance.setOutcomeUpstream(finance.getOutcomeUpstream() + value);
                    }
                    if (type.equals("R")) {
                        finance.setOutcomeRebate(finance.getOutcomeRebate() + value);
                    }
                    if (type.equals("D")) {
                        finance.setOutcomeDownstream(finance.getOutcomeDownstream() + value);
                    }

                    String financeBundleKey = hour + "|" + minute + "|" + clientPort + "|" + vendorPort + "|" + bundle + "|" + node;
                    if (!financeBundleMap.containsKey(financeBundleKey)) {
                        FinanceBundle financeBundle = new FinanceBundle(clientPort, vendorPort, bundle, node, new Timestamp(finance_calendar.getTimeInMillis()));

                        financeBundleList.add(financeBundle);
                        financeBundleMap.put(financeBundleKey, financeBundle);
                    }
                    FinanceBundle financeBundle = financeBundleMap.get(financeBundleKey);
                    if (type.equals("I")) {
                        financeBundle.setIncome(financeBundle.getIncome() + value);
                    }
                    if (type.equals("U")) {
                        financeBundle.setOutcomeUpstream(financeBundle.getOutcomeUpstream() + value);
                    }
                    if (type.equals("R")) {
                        financeBundle.setOutcomeRebate(financeBundle.getOutcomeRebate() + value);
                    }
                    if (type.equals("D")) {
                        financeBundle.setOutcomeDownstream(financeBundle.getOutcomeDownstream() + value);
                    }
                }
            } catch (Exception e) {
            }
        }

        performanceService.addAllPerformance(performanceList);
        performanceService.addAllPerformanceBundle(performanceBundleList);
        performanceService.addAllFinance(financeList);
        performanceService.addAllFinanceBundle(financeBundleList);

        Timestamp start1 = performanceList.stream().map(p -> p.getTime()).min(Timestamp::compareTo).orElse(null);
        Timestamp start2 = performanceBundleList.stream().map(p -> p.getTime()).min(Timestamp::compareTo).orElse(null);
        Timestamp start3 = financeList.stream().map(f -> f.getTime()).min(Timestamp::compareTo).orElse(null);
        Timestamp start4 = financeBundleList.stream().map(f -> f.getTime()).min(Timestamp::compareTo).orElse(null);
        Timestamp end1 = performanceList.stream().map(p -> p.getTime()).max(Timestamp::compareTo).orElse(null);
        Timestamp end2 = performanceBundleList.stream().map(p -> p.getTime()).max(Timestamp::compareTo).orElse(null);
        Timestamp end3 = financeList.stream().map(f -> f.getTime()).max(Timestamp::compareTo).orElse(null);
        Timestamp end4 = financeBundleList.stream().map(f -> f.getTime()).max(Timestamp::compareTo).orElse(null);

        Timestamp start = start1;
        if (start == null || (start2 != null && start.after(start2))) {
            start = start2;
        }
        if (start == null || (start3 != null && start.after(start3))) {
            start = start3;
        }
        if (start == null || (start4 != null && start.after(start4))) {
            start = start4;
        }
        Timestamp end = end1;
        if (end == null || (end2 != null && end.before(end2))) {
            end = end2;
        }
        if (end == null || (end3 != null && end.before(end3))) {
            end = end3;
        }
        if (end == null || (end4 != null && end.before(end4))) {
            end = end4;
        }

        if (start != null && end != null) {
            final Timestamp aggregateStart = start;
            final Timestamp aggregateEnd = end;
            transactionTemplate.executeWithoutResult(status ->
                aggregatePerformance(aggregateStart, aggregateEnd, "GMT+08:00", true));
            transactionTemplate.executeWithoutResult(status ->
                aggregatePerformanceBundle(aggregateStart, aggregateEnd, "GMT+08:00", true));
        }
    }

    @Transactional
    public void aggregatePerformance(Timestamp start, Timestamp end, String timezone, Boolean incremental) {
        List<Performance> performanceList = performanceService.getPerformanceList(start, end);
        List<Finance> financeList = performanceService.getFinanceList(start, end);

        ZonedDateTime startTime = start.toInstant().atZone(ZoneId.of(timezone));
        ZonedDateTime endTime = end.toInstant().atZone(ZoneId.of(timezone));
        ZonedDateTime startQuarter = startTime.withMinute(startTime.getMinute() / 15 * 15).withSecond(0).withNano(0);
        ZonedDateTime endQuarter = endTime.withMinute(endTime.getMinute() / 15 * 15).withSecond(0).withNano(0);
        ZonedDateTime startHour = startTime.withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime endHour = endTime.withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime startDay = startTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime endDay = endTime.withHour(0).withMinute(0).withSecond(0).withNano(0);

        Map<String, PerformanceClientQuarter> performanceClientQuarterMap = new HashMap<String, PerformanceClientQuarter>();
        Map<String, PerformanceClientHour> performanceClientHourMap = new HashMap<String, PerformanceClientHour>();
        Map<String, PerformanceClientDay> performanceClientDayMap = new HashMap<String, PerformanceClientDay>();
        Map<String, PerformanceVendorQuarter> performanceVendorQuarterMap = new HashMap<String, PerformanceVendorQuarter>();
        Map<String, PerformanceVendorHour> performanceVendorHourMap = new HashMap<String, PerformanceVendorHour>();
        Map<String, PerformanceVendorDay> performanceVendorDayMap = new HashMap<String, PerformanceVendorDay>();
        HashSet<String> existingPerformanceClientQuarterKeys = new HashSet<String>();
        HashSet<String> existingPerformanceVendorQuarterKeys = new HashSet<String>();

        if (incremental) {
            List<PerformanceClientQuarter> performanceClientQuarterList = performanceClientQuarterRepository.findByTimeBetween(Timestamp.from(startQuarter.toInstant()), Timestamp.from(endQuarter.toInstant()));
            List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findByTimeBetween(Timestamp.from(startHour.toInstant()), Timestamp.from(endHour.toInstant()));
            List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findByTimeBetween(Timestamp.from(startDay.toInstant()), Timestamp.from(endDay.toInstant()));
            List<PerformanceVendorQuarter> performanceVendorQuarterList = performanceVendorQuarterRepository.findByTimeBetween(Timestamp.from(startQuarter.toInstant()), Timestamp.from(endQuarter.toInstant()));
            List<PerformanceVendorHour> performanceVendorHourList = performanceVendorHourRepository.findByTimeBetween(Timestamp.from(startHour.toInstant()), Timestamp.from(endHour.toInstant()));
            List<PerformanceVendorDay> performanceVendorDayList = performanceVendorDayRepository.findByTimeBetween(Timestamp.from(startDay.toInstant()), Timestamp.from(endDay.toInstant()));

            for (PerformanceClientQuarter performanceClientQuarter : performanceClientQuarterList) {
                entityManager.detach(performanceClientQuarter);
                performanceClientQuarter.setId(null);

                if (performanceClientQuarter.getVendorPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientQuarter.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientQuarter.getClientPort() + "|" + performanceClientQuarter.getVendorPort();
                    existingPerformanceClientQuarterKeys.add(key);
                    performanceClientQuarterMap.put(key, performanceClientQuarter);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientQuarter.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientQuarter.getClientPort();
                    performanceClientQuarterMap.put(key, performanceClientQuarter);
                }
            }
            for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                entityManager.detach(performanceClientHour);
                performanceClientHour.setId(null);

                if (performanceClientHour.getVendorPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientHour.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientHour.getClientPort() + "|" + performanceClientHour.getVendorPort();
                    performanceClientHourMap.put(key, performanceClientHour);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientHour.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientHour.getClientPort();
                    performanceClientHourMap.put(key, performanceClientHour);
                }
            }
            for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                entityManager.detach(performanceClientDay);
                performanceClientDay.setId(null);

                if (performanceClientDay.getVendorPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientDay.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientDay.getClientPort() + "|" + performanceClientDay.getVendorPort();
                    performanceClientDayMap.put(key, performanceClientDay);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientDay.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientDay.getClientPort();
                    performanceClientDayMap.put(key, performanceClientDay);
                }
            }
            for (PerformanceVendorQuarter performanceVendorQuarter : performanceVendorQuarterList) {
                entityManager.detach(performanceVendorQuarter);
                performanceVendorQuarter.setId(null);

                if (performanceVendorQuarter.getClientPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorQuarter.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorQuarter.getClientPort() + "|" + performanceVendorQuarter.getVendorPort();
                    existingPerformanceVendorQuarterKeys.add(key);
                    performanceVendorQuarterMap.put(key, performanceVendorQuarter);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorQuarter.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorQuarter.getVendorPort();
                    performanceVendorQuarterMap.put(key, performanceVendorQuarter);
                }
            }
            for (PerformanceVendorHour performanceVendorHour : performanceVendorHourList) {
                entityManager.detach(performanceVendorHour);
                performanceVendorHour.setId(null);

                if (performanceVendorHour.getClientPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorHour.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorHour.getClientPort() + "|" + performanceVendorHour.getVendorPort();
                    performanceVendorHourMap.put(key, performanceVendorHour);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorHour.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorHour.getVendorPort();
                    performanceVendorHourMap.put(key, performanceVendorHour);
                }
            }
            for (PerformanceVendorDay performanceVendorDay : performanceVendorDayList) {
                entityManager.detach(performanceVendorDay);
                performanceVendorDay.setId(null);

                if (performanceVendorDay.getClientPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorDay.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorDay.getClientPort() + "|" + performanceVendorDay.getVendorPort();
                    performanceVendorDayMap.put(key, performanceVendorDay);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorDay.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorDay.getVendorPort();
                    performanceVendorDayMap.put(key, performanceVendorDay);
                }
            }
        }

        performanceClientQuarterRepository.deleteByTimeBetween(Timestamp.from(startQuarter.toInstant()), Timestamp.from(endQuarter.toInstant()));
        performanceClientHourRepository.deleteByTimeBetween(Timestamp.from(startHour.toInstant()), Timestamp.from(endHour.toInstant()));
        performanceClientDayRepository.deleteByTimeBetween(Timestamp.from(startDay.toInstant()), Timestamp.from(endDay.toInstant()));
        performanceVendorQuarterRepository.deleteByTimeBetween(Timestamp.from(startQuarter.toInstant()), Timestamp.from(endQuarter.toInstant()));
        performanceVendorHourRepository.deleteByTimeBetween(Timestamp.from(startHour.toInstant()), Timestamp.from(endHour.toInstant()));
        performanceVendorDayRepository.deleteByTimeBetween(Timestamp.from(startDay.toInstant()), Timestamp.from(endDay.toInstant()));

        for (Performance performance : performanceList) {
            ZonedDateTime time = performance.getTime().toInstant().atZone(ZoneId.of(timezone));
            ZonedDateTime timeQuarter = time.withMinute(time.getMinute() / 15 * 15).withSecond(0).withNano(0);
            ZonedDateTime timeHour = time.withMinute(0).withSecond(0).withNano(0);
            ZonedDateTime timeDay = time.withHour(0).withMinute(0).withSecond(0).withNano(0);
            String existingQuarterKey = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
            char eventType = performance.getEvent().charAt(0);

            if (incremental && (
                    eventType == 'Z' && existingPerformanceClientQuarterKeys.contains(existingQuarterKey) ||
                    "ABCDEFGHIJ".indexOf(eventType) >= 0 && existingPerformanceVendorQuarterKeys.contains(existingQuarterKey) ||
                    "abz".indexOf(eventType) >= 0 && (existingPerformanceClientQuarterKeys.contains(existingQuarterKey) || existingPerformanceVendorQuarterKeys.contains(existingQuarterKey)))) {
                continue;
            }

            if ("Z".indexOf(performance.getEvent().charAt(0)) >= 0) {
                String keyClientQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceClientQuarterMap.containsKey(keyClientQuarter)) {
                    PerformanceClientQuarter performanceClientQuarter = new PerformanceClientQuarter(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                    performanceClientQuarterMap.put(keyClientQuarter, performanceClientQuarter);
                }
                PerformanceClientQuarter performanceClientQuarter = performanceClientQuarterMap.get(keyClientQuarter);
                switch (performance.getEvent().substring(1, 2)) {
                    case "A": performanceClientQuarter.setEventA(performanceClientQuarter.getEventA() + performance.getAmount());break;
                    case "B": performanceClientQuarter.setEventB(performanceClientQuarter.getEventB() + performance.getAmount());break;
                    case "C": performanceClientQuarter.setEventC(performanceClientQuarter.getEventC() + performance.getAmount());break;
                    case "D": performanceClientQuarter.setEventD(performanceClientQuarter.getEventD() + performance.getAmount());break;
                    case "E": performanceClientQuarter.setEventE(performanceClientQuarter.getEventE() + performance.getAmount());break;
                    case "F": performanceClientQuarter.setEventF(performanceClientQuarter.getEventF() + performance.getAmount());break;
                    case "G": performanceClientQuarter.setEventG(performanceClientQuarter.getEventG() + performance.getAmount());break;
                    case "H": performanceClientQuarter.setEventH(performanceClientQuarter.getEventH() + performance.getAmount());break;
                    case "I": performanceClientQuarter.setEventI(performanceClientQuarter.getEventI() + performance.getAmount());break;
                    case "J": performanceClientQuarter.setEventJ(performanceClientQuarter.getEventJ() + performance.getAmount());break;
                    case "K": performanceClientQuarter.setEventK(performanceClientQuarter.getEventK() + performance.getAmount());break;
                    case "L": performanceClientQuarter.setEventL(performanceClientQuarter.getEventL() + performance.getAmount());break;
                    case "M": performanceClientQuarter.setEventM(performanceClientQuarter.getEventM() + performance.getAmount());break;
                    case "N": performanceClientQuarter.setEventN(performanceClientQuarter.getEventN() + performance.getAmount());break;
                    default: break;
                }

                String keyClientHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceClientHourMap.containsKey(keyClientHour)) {
                    PerformanceClientHour performanceClientHour = new PerformanceClientHour(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                    performanceClientHourMap.put(keyClientHour, performanceClientHour);
                }
                PerformanceClientHour performanceClientHour = performanceClientHourMap.get(keyClientHour);
                switch (performance.getEvent().substring(1, 2)) {
                    case "A": performanceClientHour.setEventA(performanceClientHour.getEventA() + performance.getAmount());break;
                    case "B": performanceClientHour.setEventB(performanceClientHour.getEventB() + performance.getAmount());break;
                    case "C": performanceClientHour.setEventC(performanceClientHour.getEventC() + performance.getAmount());break;
                    case "D": performanceClientHour.setEventD(performanceClientHour.getEventD() + performance.getAmount());break;
                    case "E": performanceClientHour.setEventE(performanceClientHour.getEventE() + performance.getAmount());break;
                    case "F": performanceClientHour.setEventF(performanceClientHour.getEventF() + performance.getAmount());break;
                    case "G": performanceClientHour.setEventG(performanceClientHour.getEventG() + performance.getAmount());break;
                    case "H": performanceClientHour.setEventH(performanceClientHour.getEventH() + performance.getAmount());break;
                    case "I": performanceClientHour.setEventI(performanceClientHour.getEventI() + performance.getAmount());break;
                    case "J": performanceClientHour.setEventJ(performanceClientHour.getEventJ() + performance.getAmount());break;
                    case "K": performanceClientHour.setEventK(performanceClientHour.getEventK() + performance.getAmount());break;
                    case "L": performanceClientHour.setEventL(performanceClientHour.getEventL() + performance.getAmount());break;
                    case "M": performanceClientHour.setEventM(performanceClientHour.getEventM() + performance.getAmount());break;
                    case "N": performanceClientHour.setEventN(performanceClientHour.getEventN() + performance.getAmount());break;
                    default: break;
                }

                String keyClientDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceClientDayMap.containsKey(keyClientDay)) {
                    PerformanceClientDay performanceClientDay = new PerformanceClientDay(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                    performanceClientDayMap.put(keyClientDay, performanceClientDay);
                }
                PerformanceClientDay performanceClientDay = performanceClientDayMap.get(keyClientDay);
                switch (performance.getEvent().substring(1, 2)) {
                    case "A": performanceClientDay.setEventA(performanceClientDay.getEventA() + performance.getAmount());break;
                    case "B": performanceClientDay.setEventB(performanceClientDay.getEventB() + performance.getAmount());break;
                    case "C": performanceClientDay.setEventC(performanceClientDay.getEventC() + performance.getAmount());break;
                    case "D": performanceClientDay.setEventD(performanceClientDay.getEventD() + performance.getAmount());break;
                    case "E": performanceClientDay.setEventE(performanceClientDay.getEventE() + performance.getAmount());break;
                    case "F": performanceClientDay.setEventF(performanceClientDay.getEventF() + performance.getAmount());break;
                    case "G": performanceClientDay.setEventG(performanceClientDay.getEventG() + performance.getAmount());break;
                    case "H": performanceClientDay.setEventH(performanceClientDay.getEventH() + performance.getAmount());break;
                    case "I": performanceClientDay.setEventI(performanceClientDay.getEventI() + performance.getAmount());break;
                    case "J": performanceClientDay.setEventJ(performanceClientDay.getEventJ() + performance.getAmount());break;
                    case "K": performanceClientDay.setEventK(performanceClientDay.getEventK() + performance.getAmount());break;
                    case "L": performanceClientDay.setEventL(performanceClientDay.getEventL() + performance.getAmount());break;
                    case "M": performanceClientDay.setEventM(performanceClientDay.getEventM() + performance.getAmount());break;
                    case "N": performanceClientDay.setEventN(performanceClientDay.getEventN() + performance.getAmount());break;
                    default: break;
                }

                String keyClientQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getClientPort();
                if (!performanceClientQuarterMap.containsKey(keyClientQuarter0)) {
                    PerformanceClientQuarter performanceClientQuarter0 = new PerformanceClientQuarter(performance.getClientPort(), 0, Timestamp.from(timeQuarter.toInstant()));
                    performanceClientQuarterMap.put(keyClientQuarter0, performanceClientQuarter0);
                }
                PerformanceClientQuarter performanceClientQuarter0 = performanceClientQuarterMap.get(keyClientQuarter0);
                switch (performance.getEvent().substring(1, 2)) {
                    case "A": performanceClientQuarter0.setEventA(performanceClientQuarter0.getEventA() + performance.getAmount());break;
                    case "B": performanceClientQuarter0.setEventB(performanceClientQuarter0.getEventB() + performance.getAmount());break;
                    case "C": performanceClientQuarter0.setEventC(performanceClientQuarter0.getEventC() + performance.getAmount());break;
                    case "D": performanceClientQuarter0.setEventD(performanceClientQuarter0.getEventD() + performance.getAmount());break;
                    case "E": performanceClientQuarter0.setEventE(performanceClientQuarter0.getEventE() + performance.getAmount());break;
                    case "F": performanceClientQuarter0.setEventF(performanceClientQuarter0.getEventF() + performance.getAmount());break;
                    case "G": performanceClientQuarter0.setEventG(performanceClientQuarter0.getEventG() + performance.getAmount());break;
                    case "H": performanceClientQuarter0.setEventH(performanceClientQuarter0.getEventH() + performance.getAmount());break;
                    case "I": performanceClientQuarter0.setEventI(performanceClientQuarter0.getEventI() + performance.getAmount());break;
                    case "J": performanceClientQuarter0.setEventJ(performanceClientQuarter0.getEventJ() + performance.getAmount());break;
                    case "K": performanceClientQuarter0.setEventK(performanceClientQuarter0.getEventK() + performance.getAmount());break;
                    case "L": performanceClientQuarter0.setEventL(performanceClientQuarter0.getEventL() + performance.getAmount());break;
                    case "M": performanceClientQuarter0.setEventM(performanceClientQuarter0.getEventM() + performance.getAmount());break;
                    case "N": performanceClientQuarter0.setEventN(performanceClientQuarter0.getEventN() + performance.getAmount());break;
                    default: break;
                }

                String keyClientHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performance.getClientPort();
                if (!performanceClientHourMap.containsKey(keyClientHour0)) {
                    PerformanceClientHour performanceClientHour0 = new PerformanceClientHour(performance.getClientPort(), 0, Timestamp.from(timeHour.toInstant()));
                    performanceClientHourMap.put(keyClientHour0, performanceClientHour0);
                }
                PerformanceClientHour performanceClientHour0 = performanceClientHourMap.get(keyClientHour0);
                switch (performance.getEvent().substring(1, 2)) {
                    case "A": performanceClientHour0.setEventA(performanceClientHour0.getEventA() + performance.getAmount());break;
                    case "B": performanceClientHour0.setEventB(performanceClientHour0.getEventB() + performance.getAmount());break;
                    case "C": performanceClientHour0.setEventC(performanceClientHour0.getEventC() + performance.getAmount());break;
                    case "D": performanceClientHour0.setEventD(performanceClientHour0.getEventD() + performance.getAmount());break;
                    case "E": performanceClientHour0.setEventE(performanceClientHour0.getEventE() + performance.getAmount());break;
                    case "F": performanceClientHour0.setEventF(performanceClientHour0.getEventF() + performance.getAmount());break;
                    case "G": performanceClientHour0.setEventG(performanceClientHour0.getEventG() + performance.getAmount());break;
                    case "H": performanceClientHour0.setEventH(performanceClientHour0.getEventH() + performance.getAmount());break;
                    case "I": performanceClientHour0.setEventI(performanceClientHour0.getEventI() + performance.getAmount());break;
                    case "J": performanceClientHour0.setEventJ(performanceClientHour0.getEventJ() + performance.getAmount());break;
                    case "K": performanceClientHour0.setEventK(performanceClientHour0.getEventK() + performance.getAmount());break;
                    case "L": performanceClientHour0.setEventL(performanceClientHour0.getEventL() + performance.getAmount());break;
                    case "M": performanceClientHour0.setEventM(performanceClientHour0.getEventM() + performance.getAmount());break;
                    case "N": performanceClientHour0.setEventN(performanceClientHour0.getEventN() + performance.getAmount());break;
                    default: break;
                }

                String keyClientDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performance.getClientPort();
                if (!performanceClientDayMap.containsKey(keyClientDay0)) {
                    PerformanceClientDay performanceClientDay0 = new PerformanceClientDay(performance.getClientPort(), 0, Timestamp.from(timeDay.toInstant()));
                    performanceClientDayMap.put(keyClientDay0, performanceClientDay0);
                }
                PerformanceClientDay performanceClientDay0 = performanceClientDayMap.get(keyClientDay0);
                switch (performance.getEvent().substring(1, 2)) {
                    case "A": performanceClientDay0.setEventA(performanceClientDay0.getEventA() + performance.getAmount());break;
                    case "B": performanceClientDay0.setEventB(performanceClientDay0.getEventB() + performance.getAmount());break;
                    case "C": performanceClientDay0.setEventC(performanceClientDay0.getEventC() + performance.getAmount());break;
                    case "D": performanceClientDay0.setEventD(performanceClientDay0.getEventD() + performance.getAmount());break;
                    case "E": performanceClientDay0.setEventE(performanceClientDay0.getEventE() + performance.getAmount());break;
                    case "F": performanceClientDay0.setEventF(performanceClientDay0.getEventF() + performance.getAmount());break;
                    case "G": performanceClientDay0.setEventG(performanceClientDay0.getEventG() + performance.getAmount());break;
                    case "H": performanceClientDay0.setEventH(performanceClientDay0.getEventH() + performance.getAmount());break;
                    case "I": performanceClientDay0.setEventI(performanceClientDay0.getEventI() + performance.getAmount());break;
                    case "J": performanceClientDay0.setEventJ(performanceClientDay0.getEventJ() + performance.getAmount());break;
                    case "K": performanceClientDay0.setEventK(performanceClientDay0.getEventK() + performance.getAmount());break;
                    case "L": performanceClientDay0.setEventL(performanceClientDay0.getEventL() + performance.getAmount());break;
                    case "M": performanceClientDay0.setEventM(performanceClientDay0.getEventM() + performance.getAmount());break;
                    case "N": performanceClientDay0.setEventN(performanceClientDay0.getEventN() + performance.getAmount());break;
                    default: break;
                }
            }
            if ("ABCDEFGHIJ".indexOf(performance.getEvent().charAt(0)) >= 0) {
                String keyVendorQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceVendorQuarterMap.containsKey(keyVendorQuarter)) {
                    PerformanceVendorQuarter performanceVendorQuarter = new PerformanceVendorQuarter(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                    performanceVendorQuarterMap.put(keyVendorQuarter, performanceVendorQuarter);
                }
                PerformanceVendorQuarter performanceVendorQuarter = performanceVendorQuarterMap.get(keyVendorQuarter);
                switch (performance.getEvent().substring(0, 1)) {
                    case "A": performanceVendorQuarter.setEventA(performanceVendorQuarter.getEventA() + performance.getAmount());break;
                    case "B": performanceVendorQuarter.setEventB(performanceVendorQuarter.getEventB() + performance.getAmount());break;
                    case "C": performanceVendorQuarter.setEventC(performanceVendorQuarter.getEventC() + performance.getAmount());break;
                    case "D": performanceVendorQuarter.setEventD(performanceVendorQuarter.getEventD() + performance.getAmount());break;
                    case "E": performanceVendorQuarter.setEventE(performanceVendorQuarter.getEventE() + performance.getAmount());break;
                    case "F": performanceVendorQuarter.setEventF(performanceVendorQuarter.getEventF() + performance.getAmount());break;
                    case "G": performanceVendorQuarter.setEventG(performanceVendorQuarter.getEventG() + performance.getAmount());break;
                    case "H": performanceVendorQuarter.setEventH(performanceVendorQuarter.getEventH() + performance.getAmount());break;
                    case "I": performanceVendorQuarter.setEventI(performanceVendorQuarter.getEventI() + performance.getAmount());break;
                    case "J": performanceVendorQuarter.setEventJ(performanceVendorQuarter.getEventJ() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceVendorHourMap.containsKey(keyVendorHour)) {
                    PerformanceVendorHour performanceVendorHour = new PerformanceVendorHour(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                    performanceVendorHourMap.put(keyVendorHour, performanceVendorHour);
                }
                PerformanceVendorHour performanceVendorHour = performanceVendorHourMap.get(keyVendorHour);
                switch (performance.getEvent().substring(0, 1)) {
                    case "A": performanceVendorHour.setEventA(performanceVendorHour.getEventA() + performance.getAmount());break;
                    case "B": performanceVendorHour.setEventB(performanceVendorHour.getEventB() + performance.getAmount());break;
                    case "C": performanceVendorHour.setEventC(performanceVendorHour.getEventC() + performance.getAmount());break;
                    case "D": performanceVendorHour.setEventD(performanceVendorHour.getEventD() + performance.getAmount());break;
                    case "E": performanceVendorHour.setEventE(performanceVendorHour.getEventE() + performance.getAmount());break;
                    case "F": performanceVendorHour.setEventF(performanceVendorHour.getEventF() + performance.getAmount());break;
                    case "G": performanceVendorHour.setEventG(performanceVendorHour.getEventG() + performance.getAmount());break;
                    case "H": performanceVendorHour.setEventH(performanceVendorHour.getEventH() + performance.getAmount());break;
                    case "I": performanceVendorHour.setEventI(performanceVendorHour.getEventI() + performance.getAmount());break;
                    case "J": performanceVendorHour.setEventJ(performanceVendorHour.getEventJ() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceVendorDayMap.containsKey(keyVendorDay)) {
                    PerformanceVendorDay performanceVendorDay = new PerformanceVendorDay(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                    performanceVendorDayMap.put(keyVendorDay, performanceVendorDay);
                }
                PerformanceVendorDay performanceVendorDay = performanceVendorDayMap.get(keyVendorDay);
                switch (performance.getEvent().substring(0, 1)) {
                    case "A": performanceVendorDay.setEventA(performanceVendorDay.getEventA() + performance.getAmount());break;
                    case "B": performanceVendorDay.setEventB(performanceVendorDay.getEventB() + performance.getAmount());break;
                    case "C": performanceVendorDay.setEventC(performanceVendorDay.getEventC() + performance.getAmount());break;
                    case "D": performanceVendorDay.setEventD(performanceVendorDay.getEventD() + performance.getAmount());break;
                    case "E": performanceVendorDay.setEventE(performanceVendorDay.getEventE() + performance.getAmount());break;
                    case "F": performanceVendorDay.setEventF(performanceVendorDay.getEventF() + performance.getAmount());break;
                    case "G": performanceVendorDay.setEventG(performanceVendorDay.getEventG() + performance.getAmount());break;
                    case "H": performanceVendorDay.setEventH(performanceVendorDay.getEventH() + performance.getAmount());break;
                    case "I": performanceVendorDay.setEventI(performanceVendorDay.getEventI() + performance.getAmount());break;
                    case "J": performanceVendorDay.setEventJ(performanceVendorDay.getEventJ() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getVendorPort();
                if (!performanceVendorQuarterMap.containsKey(keyVendorQuarter0)) {
                    PerformanceVendorQuarter performanceVendorQuarter0 = new PerformanceVendorQuarter(0, performance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                    performanceVendorQuarterMap.put(keyVendorQuarter0, performanceVendorQuarter0);
                }
                PerformanceVendorQuarter performanceVendorQuarter0 = performanceVendorQuarterMap.get(keyVendorQuarter0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "A": performanceVendorQuarter0.setEventA(performanceVendorQuarter0.getEventA() + performance.getAmount());break;
                    case "B": performanceVendorQuarter0.setEventB(performanceVendorQuarter0.getEventB() + performance.getAmount());break;
                    case "C": performanceVendorQuarter0.setEventC(performanceVendorQuarter0.getEventC() + performance.getAmount());break;
                    case "D": performanceVendorQuarter0.setEventD(performanceVendorQuarter0.getEventD() + performance.getAmount());break;
                    case "E": performanceVendorQuarter0.setEventE(performanceVendorQuarter0.getEventE() + performance.getAmount());break;
                    case "F": performanceVendorQuarter0.setEventF(performanceVendorQuarter0.getEventF() + performance.getAmount());break;
                    case "G": performanceVendorQuarter0.setEventG(performanceVendorQuarter0.getEventG() + performance.getAmount());break;
                    case "H": performanceVendorQuarter0.setEventH(performanceVendorQuarter0.getEventH() + performance.getAmount());break;
                    case "I": performanceVendorQuarter0.setEventI(performanceVendorQuarter0.getEventI() + performance.getAmount());break;
                    case "J": performanceVendorQuarter0.setEventJ(performanceVendorQuarter0.getEventJ() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performance.getVendorPort();
                if (!performanceVendorHourMap.containsKey(keyVendorHour0)) {
                    PerformanceVendorHour performanceVendorHour0 = new PerformanceVendorHour(0, performance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                    performanceVendorHourMap.put(keyVendorHour0, performanceVendorHour0);
                }
                PerformanceVendorHour performanceVendorHour0 = performanceVendorHourMap.get(keyVendorHour0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "A": performanceVendorHour0.setEventA(performanceVendorHour0.getEventA() + performance.getAmount());break;
                    case "B": performanceVendorHour0.setEventB(performanceVendorHour0.getEventB() + performance.getAmount());break;
                    case "C": performanceVendorHour0.setEventC(performanceVendorHour0.getEventC() + performance.getAmount());break;
                    case "D": performanceVendorHour0.setEventD(performanceVendorHour0.getEventD() + performance.getAmount());break;
                    case "E": performanceVendorHour0.setEventE(performanceVendorHour0.getEventE() + performance.getAmount());break;
                    case "F": performanceVendorHour0.setEventF(performanceVendorHour0.getEventF() + performance.getAmount());break;
                    case "G": performanceVendorHour0.setEventG(performanceVendorHour0.getEventG() + performance.getAmount());break;
                    case "H": performanceVendorHour0.setEventH(performanceVendorHour0.getEventH() + performance.getAmount());break;
                    case "I": performanceVendorHour0.setEventI(performanceVendorHour0.getEventI() + performance.getAmount());break;
                    case "J": performanceVendorHour0.setEventJ(performanceVendorHour0.getEventJ() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performance.getVendorPort();
                if (!performanceVendorDayMap.containsKey(keyVendorDay0)) {
                    PerformanceVendorDay performanceVendorDay0 = new PerformanceVendorDay(0, performance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                    performanceVendorDayMap.put(keyVendorDay0, performanceVendorDay0);
                }
                PerformanceVendorDay performanceVendorDay0 = performanceVendorDayMap.get(keyVendorDay0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "A": performanceVendorDay0.setEventA(performanceVendorDay0.getEventA() + performance.getAmount());break;
                    case "B": performanceVendorDay0.setEventB(performanceVendorDay0.getEventB() + performance.getAmount());break;
                    case "C": performanceVendorDay0.setEventC(performanceVendorDay0.getEventC() + performance.getAmount());break;
                    case "D": performanceVendorDay0.setEventD(performanceVendorDay0.getEventD() + performance.getAmount());break;
                    case "E": performanceVendorDay0.setEventE(performanceVendorDay0.getEventE() + performance.getAmount());break;
                    case "F": performanceVendorDay0.setEventF(performanceVendorDay0.getEventF() + performance.getAmount());break;
                    case "G": performanceVendorDay0.setEventG(performanceVendorDay0.getEventG() + performance.getAmount());break;
                    case "H": performanceVendorDay0.setEventH(performanceVendorDay0.getEventH() + performance.getAmount());break;
                    case "I": performanceVendorDay0.setEventI(performanceVendorDay0.getEventI() + performance.getAmount());break;
                    case "J": performanceVendorDay0.setEventJ(performanceVendorDay0.getEventJ() + performance.getAmount());break;
                    default: break;
                }
            }
            if ("ab".indexOf(performance.getEvent().charAt(0)) >= 0) {
                String keyClientQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceClientQuarterMap.containsKey(keyClientQuarter)) {
                    PerformanceClientQuarter performanceClientQuarter = new PerformanceClientQuarter(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                    performanceClientQuarterMap.put(keyClientQuarter, performanceClientQuarter);
                }
                PerformanceClientQuarter performanceClientQuarter = performanceClientQuarterMap.get(keyClientQuarter);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceClientQuarter.setImpression(performanceClientQuarter.getImpression() + performance.getAmount());break;
                    case "b": performanceClientQuarter.setClick(performanceClientQuarter.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyClientHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceClientHourMap.containsKey(keyClientHour)) {
                    PerformanceClientHour performanceClientHour = new PerformanceClientHour(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                    performanceClientHourMap.put(keyClientHour, performanceClientHour);
                }
                PerformanceClientHour performanceClientHour = performanceClientHourMap.get(keyClientHour);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceClientHour.setImpression(performanceClientHour.getImpression() + performance.getAmount());break;
                    case "b": performanceClientHour.setClick(performanceClientHour.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyClientDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceClientDayMap.containsKey(keyClientDay)) {
                    PerformanceClientDay performanceClientDay = new PerformanceClientDay(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                    performanceClientDayMap.put(keyClientDay, performanceClientDay);
                }
                PerformanceClientDay performanceClientDay = performanceClientDayMap.get(keyClientDay);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceClientDay.setImpression(performanceClientDay.getImpression() + performance.getAmount());break;
                    case "b": performanceClientDay.setClick(performanceClientDay.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceVendorQuarterMap.containsKey(keyVendorQuarter)) {
                    PerformanceVendorQuarter performanceVendorQuarter = new PerformanceVendorQuarter(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                    performanceVendorQuarterMap.put(keyVendorQuarter, performanceVendorQuarter);
                }
                PerformanceVendorQuarter performanceVendorQuarter = performanceVendorQuarterMap.get(keyVendorQuarter);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceVendorQuarter.setImpression(performanceVendorQuarter.getImpression() + performance.getAmount());break;
                    case "b": performanceVendorQuarter.setClick(performanceVendorQuarter.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceVendorHourMap.containsKey(keyVendorHour)) {
                    PerformanceVendorHour performanceVendorHour = new PerformanceVendorHour(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                    performanceVendorHourMap.put(keyVendorHour, performanceVendorHour);
                }
                PerformanceVendorHour performanceVendorHour = performanceVendorHourMap.get(keyVendorHour);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceVendorHour.setImpression(performanceVendorHour.getImpression() + performance.getAmount());break;
                    case "b": performanceVendorHour.setClick(performanceVendorHour.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performance.getClientPort() + "|" + performance.getVendorPort();
                if (!performanceVendorDayMap.containsKey(keyVendorDay)) {
                    PerformanceVendorDay performanceVendorDay = new PerformanceVendorDay(performance.getClientPort(), performance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                    performanceVendorDayMap.put(keyVendorDay, performanceVendorDay);
                }
                PerformanceVendorDay performanceVendorDay = performanceVendorDayMap.get(keyVendorDay);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceVendorDay.setImpression(performanceVendorDay.getImpression() + performance.getAmount());break;
                    case "b": performanceVendorDay.setClick(performanceVendorDay.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyClientQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getClientPort();
                if (!performanceClientQuarterMap.containsKey(keyClientQuarter0)) {
                    PerformanceClientQuarter performanceClientQuarter0 = new PerformanceClientQuarter(performance.getClientPort(), 0, Timestamp.from(timeQuarter.toInstant()));
                    performanceClientQuarterMap.put(keyClientQuarter0, performanceClientQuarter0);
                }
                PerformanceClientQuarter performanceClientQuarter0 = performanceClientQuarterMap.get(keyClientQuarter0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceClientQuarter0.setImpression(performanceClientQuarter0.getImpression() + performance.getAmount());break;
                    case "b": performanceClientQuarter0.setClick(performanceClientQuarter0.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyClientHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performance.getClientPort();
                if (!performanceClientHourMap.containsKey(keyClientHour0)) {
                    PerformanceClientHour performanceClientHour0 = new PerformanceClientHour(performance.getClientPort(), 0, Timestamp.from(timeHour.toInstant()));
                    performanceClientHourMap.put(keyClientHour0, performanceClientHour0);
                }
                PerformanceClientHour performanceClientHour0 = performanceClientHourMap.get(keyClientHour0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceClientHour0.setImpression(performanceClientHour0.getImpression() + performance.getAmount());break;
                    case "b": performanceClientHour0.setClick(performanceClientHour0.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyClientDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performance.getClientPort();
                if (!performanceClientDayMap.containsKey(keyClientDay0)) {
                    PerformanceClientDay performanceClientDay0 = new PerformanceClientDay(performance.getClientPort(), 0, Timestamp.from(timeDay.toInstant()));
                    performanceClientDayMap.put(keyClientDay0, performanceClientDay0);
                }
                PerformanceClientDay performanceClientDay0 = performanceClientDayMap.get(keyClientDay0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceClientDay0.setImpression(performanceClientDay0.getImpression() + performance.getAmount());break;
                    case "b": performanceClientDay0.setClick(performanceClientDay0.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performance.getVendorPort();
                if (!performanceVendorQuarterMap.containsKey(keyVendorQuarter0)) {
                    PerformanceVendorQuarter performanceVendorQuarter0 = new PerformanceVendorQuarter(0, performance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                    performanceVendorQuarterMap.put(keyVendorQuarter0, performanceVendorQuarter0);
                }
                PerformanceVendorQuarter performanceVendorQuarter0 = performanceVendorQuarterMap.get(keyVendorQuarter0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceVendorQuarter0.setImpression(performanceVendorQuarter0.getImpression() + performance.getAmount());break;
                    case "b": performanceVendorQuarter0.setClick(performanceVendorQuarter0.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performance.getVendorPort();
                if (!performanceVendorHourMap.containsKey(keyVendorHour0)) {
                    PerformanceVendorHour performanceVendorHour0 = new PerformanceVendorHour(0, performance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                    performanceVendorHourMap.put(keyVendorHour0, performanceVendorHour0);
                }
                PerformanceVendorHour performanceVendorHour0 = performanceVendorHourMap.get(keyVendorHour0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceVendorHour0.setImpression(performanceVendorHour0.getImpression() + performance.getAmount());break;
                    case "b": performanceVendorHour0.setClick(performanceVendorHour0.getClick() + performance.getAmount());break;
                    default: break;
                }

                String keyVendorDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performance.getVendorPort();
                if (!performanceVendorDayMap.containsKey(keyVendorDay0)) {
                    PerformanceVendorDay performanceVendorDay0 = new PerformanceVendorDay(0, performance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                    performanceVendorDayMap.put(keyVendorDay0, performanceVendorDay0);
                }
                PerformanceVendorDay performanceVendorDay0 = performanceVendorDayMap.get(keyVendorDay0);
                switch (performance.getEvent().substring(0, 1)) {
                    case "a": performanceVendorDay0.setImpression(performanceVendorDay0.getImpression() + performance.getAmount());break;
                    case "b": performanceVendorDay0.setClick(performanceVendorDay0.getClick() + performance.getAmount());break;
                    default: break;
                }
            }
        }
        for (Finance finance : financeList) {
            ZonedDateTime time = finance.getTime().toInstant().atZone(ZoneId.of(timezone));
            ZonedDateTime timeQuarter = time.withMinute(time.getMinute() / 15 * 15).withSecond(0).withNano(0);
            ZonedDateTime timeHour = time.withMinute(0).withSecond(0).withNano(0);
            ZonedDateTime timeDay = time.withHour(0).withMinute(0).withSecond(0).withNano(0);
            String existingQuarterKey = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + finance.getClientPort() + "|" + finance.getVendorPort();

            if (incremental && (existingPerformanceClientQuarterKeys.contains(existingQuarterKey) || existingPerformanceVendorQuarterKeys.contains(existingQuarterKey))) {
                continue;
            }

            String keyClientQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + finance.getClientPort() + "|" + finance.getVendorPort();
            if (!performanceClientQuarterMap.containsKey(keyClientQuarter)) {
                PerformanceClientQuarter performanceClientQuarter = new PerformanceClientQuarter(finance.getClientPort(), finance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                performanceClientQuarterMap.put(keyClientQuarter, performanceClientQuarter);
            }
            PerformanceClientQuarter performanceClientQuarter = performanceClientQuarterMap.get(keyClientQuarter);
            performanceClientQuarter.setIncome(performanceClientQuarter.getIncome() + finance.getIncome());
            performanceClientQuarter.setOutcomeUpstream(performanceClientQuarter.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceClientQuarter.setOutcomeRebate(performanceClientQuarter.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceClientQuarter.setOutcomeDownstream(performanceClientQuarter.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyClientHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + finance.getClientPort() + "|" + finance.getVendorPort();
            if (!performanceClientHourMap.containsKey(keyClientHour)) {
                PerformanceClientHour performanceClientHour = new PerformanceClientHour(finance.getClientPort(), finance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                performanceClientHourMap.put(keyClientHour, performanceClientHour);
            }
            PerformanceClientHour performanceClientHour = performanceClientHourMap.get(keyClientHour);
            performanceClientHour.setIncome(performanceClientHour.getIncome() + finance.getIncome());
            performanceClientHour.setOutcomeUpstream(performanceClientHour.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceClientHour.setOutcomeRebate(performanceClientHour.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceClientHour.setOutcomeDownstream(performanceClientHour.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyClientDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + finance.getClientPort() + "|" + finance.getVendorPort();
            if (!performanceClientDayMap.containsKey(keyClientDay)) {
                PerformanceClientDay performanceClientDay = new PerformanceClientDay(finance.getClientPort(), finance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                performanceClientDayMap.put(keyClientDay, performanceClientDay);
            }
            PerformanceClientDay performanceClientDay = performanceClientDayMap.get(keyClientDay);
            performanceClientDay.setIncome(performanceClientDay.getIncome() + finance.getIncome());
            performanceClientDay.setOutcomeUpstream(performanceClientDay.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceClientDay.setOutcomeRebate(performanceClientDay.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceClientDay.setOutcomeDownstream(performanceClientDay.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyVendorQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + finance.getClientPort() + "|" + finance.getVendorPort();
            if (!performanceVendorQuarterMap.containsKey(keyVendorQuarter)) {
                PerformanceVendorQuarter performanceVendorQuarter = new PerformanceVendorQuarter(finance.getClientPort(), finance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                performanceVendorQuarterMap.put(keyVendorQuarter, performanceVendorQuarter);
            }
            PerformanceVendorQuarter performanceVendorQuarter = performanceVendorQuarterMap.get(keyVendorQuarter);
            performanceVendorQuarter.setIncome(performanceVendorQuarter.getIncome() + finance.getIncome());
            performanceVendorQuarter.setOutcomeUpstream(performanceVendorQuarter.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceVendorQuarter.setOutcomeRebate(performanceVendorQuarter.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceVendorQuarter.setOutcomeDownstream(performanceVendorQuarter.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyVendorHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + finance.getClientPort() + "|" + finance.getVendorPort();
            if (!performanceVendorHourMap.containsKey(keyVendorHour)) {
                PerformanceVendorHour performanceVendorHour = new PerformanceVendorHour(finance.getClientPort(), finance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                performanceVendorHourMap.put(keyVendorHour, performanceVendorHour);
            }
            PerformanceVendorHour performanceVendorHour = performanceVendorHourMap.get(keyVendorHour);
            performanceVendorHour.setIncome(performanceVendorHour.getIncome() + finance.getIncome());
            performanceVendorHour.setOutcomeUpstream(performanceVendorHour.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceVendorHour.setOutcomeRebate(performanceVendorHour.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceVendorHour.setOutcomeDownstream(performanceVendorHour.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyVendorDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + finance.getClientPort() + "|" + finance.getVendorPort();
            if (!performanceVendorDayMap.containsKey(keyVendorDay)) {
                PerformanceVendorDay performanceVendorDay = new PerformanceVendorDay(finance.getClientPort(), finance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                performanceVendorDayMap.put(keyVendorDay, performanceVendorDay);
            }
            PerformanceVendorDay performanceVendorDay = performanceVendorDayMap.get(keyVendorDay);
            performanceVendorDay.setIncome(performanceVendorDay.getIncome() + finance.getIncome());
            performanceVendorDay.setOutcomeUpstream(performanceVendorDay.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceVendorDay.setOutcomeRebate(performanceVendorDay.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceVendorDay.setOutcomeDownstream(performanceVendorDay.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyClientQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + finance.getClientPort();
            if (!performanceClientQuarterMap.containsKey(keyClientQuarter0)) {
                PerformanceClientQuarter performanceClientQuarter0 = new PerformanceClientQuarter(finance.getClientPort(), 0, Timestamp.from(timeQuarter.toInstant()));
                performanceClientQuarterMap.put(keyClientQuarter0, performanceClientQuarter0);
            }
            PerformanceClientQuarter performanceClientQuarter0 = performanceClientQuarterMap.get(keyClientQuarter0);
            performanceClientQuarter0.setIncome(performanceClientQuarter0.getIncome() + finance.getIncome());
            performanceClientQuarter0.setOutcomeUpstream(performanceClientQuarter0.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceClientQuarter0.setOutcomeRebate(performanceClientQuarter0.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceClientQuarter0.setOutcomeDownstream(performanceClientQuarter0.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyClientHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + finance.getClientPort();
            if (!performanceClientHourMap.containsKey(keyClientHour0)) {
                PerformanceClientHour performanceClientHour0 = new PerformanceClientHour(finance.getClientPort(), 0, Timestamp.from(timeHour.toInstant()));
                performanceClientHourMap.put(keyClientHour0, performanceClientHour0);
            }
            PerformanceClientHour performanceClientHour0 = performanceClientHourMap.get(keyClientHour0);
            performanceClientHour0.setIncome(performanceClientHour0.getIncome() + finance.getIncome());
            performanceClientHour0.setOutcomeUpstream(performanceClientHour0.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceClientHour0.setOutcomeRebate(performanceClientHour0.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceClientHour0.setOutcomeDownstream(performanceClientHour0.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyClientDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + finance.getClientPort();
            if (!performanceClientDayMap.containsKey(keyClientDay0)) {
                PerformanceClientDay performanceClientDay0 = new PerformanceClientDay(finance.getClientPort(), 0, Timestamp.from(timeDay.toInstant()));
                performanceClientDayMap.put(keyClientDay0, performanceClientDay0);
            }
            PerformanceClientDay performanceClientDay0 = performanceClientDayMap.get(keyClientDay0);
            performanceClientDay0.setIncome(performanceClientDay0.getIncome() + finance.getIncome());
            performanceClientDay0.setOutcomeUpstream(performanceClientDay0.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceClientDay0.setOutcomeRebate(performanceClientDay0.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceClientDay0.setOutcomeDownstream(performanceClientDay0.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyVendorQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + finance.getVendorPort();
            if (!performanceVendorQuarterMap.containsKey(keyVendorQuarter0)) {
                PerformanceVendorQuarter performanceVendorQuarter0 = new PerformanceVendorQuarter(0, finance.getVendorPort(), Timestamp.from(timeQuarter.toInstant()));
                performanceVendorQuarterMap.put(keyVendorQuarter0, performanceVendorQuarter0);
            }
            PerformanceVendorQuarter performanceVendorQuarter0 = performanceVendorQuarterMap.get(keyVendorQuarter0);
            performanceVendorQuarter0.setIncome(performanceVendorQuarter0.getIncome() + finance.getIncome());
            performanceVendorQuarter0.setOutcomeUpstream(performanceVendorQuarter0.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceVendorQuarter0.setOutcomeRebate(performanceVendorQuarter0.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceVendorQuarter0.setOutcomeDownstream(performanceVendorQuarter0.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyVendorHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + finance.getVendorPort();
            if (!performanceVendorHourMap.containsKey(keyVendorHour0)) {
                PerformanceVendorHour performanceVendorHour0 = new PerformanceVendorHour(0, finance.getVendorPort(), Timestamp.from(timeHour.toInstant()));
                performanceVendorHourMap.put(keyVendorHour0, performanceVendorHour0);
            }
            PerformanceVendorHour performanceVendorHour0 = performanceVendorHourMap.get(keyVendorHour0);
            performanceVendorHour0.setIncome(performanceVendorHour0.getIncome() + finance.getIncome());
            performanceVendorHour0.setOutcomeUpstream(performanceVendorHour0.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceVendorHour0.setOutcomeRebate(performanceVendorHour0.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceVendorHour0.setOutcomeDownstream(performanceVendorHour0.getOutcomeDownstream() + finance.getOutcomeDownstream());

            String keyVendorDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + finance.getVendorPort();
            if (!performanceVendorDayMap.containsKey(keyVendorDay0)) {
                PerformanceVendorDay performanceVendorDay0 = new PerformanceVendorDay(0, finance.getVendorPort(), Timestamp.from(timeDay.toInstant()));
                performanceVendorDayMap.put(keyVendorDay0, performanceVendorDay0);
            }
            PerformanceVendorDay performanceVendorDay0 = performanceVendorDayMap.get(keyVendorDay0);
            performanceVendorDay0.setIncome(performanceVendorDay0.getIncome() + finance.getIncome());
            performanceVendorDay0.setOutcomeUpstream(performanceVendorDay0.getOutcomeUpstream() + (finance.getOutcomeUpstream() == null ? 0 : finance.getOutcomeUpstream()));
            performanceVendorDay0.setOutcomeRebate(performanceVendorDay0.getOutcomeRebate() + (finance.getOutcomeRebate() == null ? 0 : finance.getOutcomeRebate()));
            performanceVendorDay0.setOutcomeDownstream(performanceVendorDay0.getOutcomeDownstream() + finance.getOutcomeDownstream());
        }

        performanceClientQuarterRepository.saveAll(performanceClientQuarterMap.values());
        performanceClientHourRepository.saveAll(performanceClientHourMap.values());
        performanceClientDayRepository.saveAll(performanceClientDayMap.values());
        performanceVendorQuarterRepository.saveAll(performanceVendorQuarterMap.values());
        performanceVendorHourRepository.saveAll(performanceVendorHourMap.values());
        performanceVendorDayRepository.saveAll(performanceVendorDayMap.values());
    }

    @Transactional
    public void aggregatePerformanceBundle(Timestamp start, Timestamp end, String timezone, Boolean incremental) {
        List<PerformanceBundle> performanceBundleList = performanceService.getPerformanceBundleList(start, end);
        List<FinanceBundle> financeBundleList = performanceService.getFinanceBundleList(start, end);

        ZonedDateTime startTime = start.toInstant().atZone(ZoneId.of(timezone));
        ZonedDateTime endTime = end.toInstant().atZone(ZoneId.of(timezone));
        ZonedDateTime startQuarter = startTime.withMinute(startTime.getMinute() / 15 * 15).withSecond(0).withNano(0);
        ZonedDateTime endQuarter = endTime.withMinute(endTime.getMinute() / 15 * 15).withSecond(0).withNano(0);
        ZonedDateTime startHour = startTime.withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime endHour = endTime.withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime startDay = startTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime endDay = endTime.withHour(0).withMinute(0).withSecond(0).withNano(0);

        Map<String, PerformanceClientBundleQuarter> performanceClientBundleQuarterMap = new HashMap<String, PerformanceClientBundleQuarter>();
        Map<String, PerformanceClientBundleHour> performanceClientBundleHourMap = new HashMap<String, PerformanceClientBundleHour>();
        Map<String, PerformanceClientBundleDay> performanceClientBundleDayMap = new HashMap<String, PerformanceClientBundleDay>();
        Map<String, PerformanceVendorBundleQuarter> performanceVendorBundleQuarterMap = new HashMap<String, PerformanceVendorBundleQuarter>();
        Map<String, PerformanceVendorBundleHour> performanceVendorBundleHourMap = new HashMap<String, PerformanceVendorBundleHour>();
        Map<String, PerformanceVendorBundleDay> performanceVendorBundleDayMap = new HashMap<String, PerformanceVendorBundleDay>();
        HashSet<String> existingPerformanceClientBundleQuarterKeys = new HashSet<String>();
        HashSet<String> existingPerformanceVendorBundleQuarterKeys = new HashSet<String>();

        if (incremental) {
            List<PerformanceClientBundleQuarter> performanceClientBundleQuarterList = performanceClientBundleQuarterRepository.findByTimeBetween(Timestamp.from(startQuarter.toInstant()), Timestamp.from(endQuarter.toInstant()));
            List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findByTimeBetween(Timestamp.from(startHour.toInstant()), Timestamp.from(endHour.toInstant()));
            List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findByTimeBetween(Timestamp.from(startDay.toInstant()), Timestamp.from(endDay.toInstant()));
            List<PerformanceVendorBundleQuarter> performanceVendorBundleQuarterList = performanceVendorBundleQuarterRepository.findByTimeBetween(Timestamp.from(startQuarter.toInstant()), Timestamp.from(endQuarter.toInstant()));
            List<PerformanceVendorBundleHour> performanceVendorBundleHourList = performanceVendorBundleHourRepository.findByTimeBetween(Timestamp.from(startHour.toInstant()), Timestamp.from(endHour.toInstant()));
            List<PerformanceVendorBundleDay> performanceVendorBundleDayList = performanceVendorBundleDayRepository.findByTimeBetween(Timestamp.from(startDay.toInstant()), Timestamp.from(endDay.toInstant()));

            for (PerformanceClientBundleQuarter performanceClientBundleQuarter : performanceClientBundleQuarterList) {
                entityManager.detach(performanceClientBundleQuarter);
                performanceClientBundleQuarter.setId(null);

                if (performanceClientBundleQuarter.getVendorPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientBundleQuarter.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientBundleQuarter.getClientPort() + "|" + performanceClientBundleQuarter.getVendorPort() + "|" + performanceClientBundleQuarter.getBundle();
                    existingPerformanceClientBundleQuarterKeys.add(key);
                    performanceClientBundleQuarterMap.put(key, performanceClientBundleQuarter);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientBundleQuarter.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientBundleQuarter.getClientPort() + "|" + performanceClientBundleQuarter.getBundle();
                    performanceClientBundleQuarterMap.put(key, performanceClientBundleQuarter);
                }
            }
            for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                entityManager.detach(performanceClientBundleHour);
                performanceClientBundleHour.setId(null);

                if (performanceClientBundleHour.getVendorPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientBundleHour.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientBundleHour.getClientPort() + "|" + performanceClientBundleHour.getVendorPort() + "|" + performanceClientBundleHour.getBundle();
                    performanceClientBundleHourMap.put(key, performanceClientBundleHour);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientBundleHour.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientBundleHour.getClientPort() + "|" + performanceClientBundleHour.getBundle();
                    performanceClientBundleHourMap.put(key, performanceClientBundleHour);
                }
            }
            for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                entityManager.detach(performanceClientBundleDay);
                performanceClientBundleDay.setId(null);

                if (performanceClientBundleDay.getVendorPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientBundleDay.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientBundleDay.getClientPort() + "|" + performanceClientBundleDay.getVendorPort() + "|" + performanceClientBundleDay.getBundle();
                    performanceClientBundleDayMap.put(key, performanceClientBundleDay);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceClientBundleDay.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceClientBundleDay.getClientPort() + "|" + performanceClientBundleDay.getBundle();
                    performanceClientBundleDayMap.put(key, performanceClientBundleDay);
                }
            }
            for (PerformanceVendorBundleQuarter performanceVendorBundleQuarter : performanceVendorBundleQuarterList) {
                entityManager.detach(performanceVendorBundleQuarter);
                performanceVendorBundleQuarter.setId(null);

                if (performanceVendorBundleQuarter.getClientPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorBundleQuarter.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorBundleQuarter.getClientPort() + "|" + performanceVendorBundleQuarter.getVendorPort() + "|" + performanceVendorBundleQuarter.getBundle();
                    existingPerformanceVendorBundleQuarterKeys.add(key);
                    performanceVendorBundleQuarterMap.put(key, performanceVendorBundleQuarter);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorBundleQuarter.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorBundleQuarter.getVendorPort() + "|" + performanceVendorBundleQuarter.getBundle();
                    performanceVendorBundleQuarterMap.put(key, performanceVendorBundleQuarter);
                }
            }
            for (PerformanceVendorBundleHour performanceVendorBundleHour : performanceVendorBundleHourList) {
                entityManager.detach(performanceVendorBundleHour);
                performanceVendorBundleHour.setId(null);

                if (performanceVendorBundleHour.getClientPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorBundleHour.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorBundleHour.getClientPort() + "|" + performanceVendorBundleHour.getVendorPort() + "|" + performanceVendorBundleHour.getBundle();
                    performanceVendorBundleHourMap.put(key, performanceVendorBundleHour);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorBundleHour.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorBundleHour.getVendorPort() + "|" + performanceVendorBundleHour.getBundle();
                    performanceVendorBundleHourMap.put(key, performanceVendorBundleHour);
                }
            }
            for (PerformanceVendorBundleDay performanceVendorBundleDay : performanceVendorBundleDayList) {
                entityManager.detach(performanceVendorBundleDay);
                performanceVendorBundleDay.setId(null);

                if (performanceVendorBundleDay.getClientPort() != 0) {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorBundleDay.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorBundleDay.getClientPort() + "|" + performanceVendorBundleDay.getVendorPort() + "|" + performanceVendorBundleDay.getBundle();
                    performanceVendorBundleDayMap.put(key, performanceVendorBundleDay);
                } else {
                    String key = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(performanceVendorBundleDay.getTime().toInstant().atZone(ZoneId.of(timezone))) + "|" + performanceVendorBundleDay.getVendorPort() + "|" + performanceVendorBundleDay.getBundle();
                    performanceVendorBundleDayMap.put(key, performanceVendorBundleDay);
                }
            }
        }

        performanceClientBundleQuarterRepository.deleteByTimeBetween(Timestamp.from(startQuarter.toInstant()), Timestamp.from(endQuarter.toInstant()));
        performanceClientBundleHourRepository.deleteByTimeBetween(Timestamp.from(startHour.toInstant()), Timestamp.from(endHour.toInstant()));
        performanceClientBundleDayRepository.deleteByTimeBetween(Timestamp.from(startDay.toInstant()), Timestamp.from(endDay.toInstant()));
        performanceVendorBundleQuarterRepository.deleteByTimeBetween(Timestamp.from(startQuarter.toInstant()), Timestamp.from(endQuarter.toInstant()));
        performanceVendorBundleHourRepository.deleteByTimeBetween(Timestamp.from(startHour.toInstant()), Timestamp.from(endHour.toInstant()));
        performanceVendorBundleDayRepository.deleteByTimeBetween(Timestamp.from(startDay.toInstant()), Timestamp.from(endDay.toInstant()));

        for (PerformanceBundle performanceBundle : performanceBundleList) {
            ZonedDateTime time = performanceBundle.getTime().toInstant().atZone(ZoneId.of(timezone));
            ZonedDateTime timeQuarter = time.withMinute(time.getMinute() / 15 * 15).withSecond(0).withNano(0);
            ZonedDateTime timeHour = time.withMinute(0).withSecond(0).withNano(0);
            ZonedDateTime timeDay = time.withHour(0).withMinute(0).withSecond(0).withNano(0);
            String existingQuarterKey = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
            char eventType = performanceBundle.getEvent().charAt(0);

            if (incremental && (
                    eventType == 'Z' && existingPerformanceClientBundleQuarterKeys.contains(existingQuarterKey) ||
                    "ABCDEFGHIJ".indexOf(eventType) >= 0 && existingPerformanceVendorBundleQuarterKeys.contains(existingQuarterKey) ||
                    "abz".indexOf(eventType) >= 0 && (existingPerformanceClientBundleQuarterKeys.contains(existingQuarterKey) || existingPerformanceVendorBundleQuarterKeys.contains(existingQuarterKey)))) {
                continue;
            }

            if ("Z".indexOf(performanceBundle.getEvent().charAt(0)) >= 0) {
                String keyClientQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleQuarterMap.containsKey(keyClientQuarter)) {
                    PerformanceClientBundleQuarter performanceClientBundleQuarter = new PerformanceClientBundleQuarter(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                    performanceClientBundleQuarterMap.put(keyClientQuarter, performanceClientBundleQuarter);
                }
                PerformanceClientBundleQuarter performanceClientBundleQuarter = performanceClientBundleQuarterMap.get(keyClientQuarter);
                switch (performanceBundle.getEvent().substring(1, 2)) {
                    case "A": performanceClientBundleQuarter.setEventA(performanceClientBundleQuarter.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceClientBundleQuarter.setEventB(performanceClientBundleQuarter.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceClientBundleQuarter.setEventC(performanceClientBundleQuarter.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceClientBundleQuarter.setEventD(performanceClientBundleQuarter.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceClientBundleQuarter.setEventE(performanceClientBundleQuarter.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceClientBundleQuarter.setEventF(performanceClientBundleQuarter.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceClientBundleQuarter.setEventG(performanceClientBundleQuarter.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceClientBundleQuarter.setEventH(performanceClientBundleQuarter.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceClientBundleQuarter.setEventI(performanceClientBundleQuarter.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceClientBundleQuarter.setEventJ(performanceClientBundleQuarter.getEventJ() + performanceBundle.getAmount());break;
                    case "K": performanceClientBundleQuarter.setEventK(performanceClientBundleQuarter.getEventK() + performanceBundle.getAmount());break;
                    case "L": performanceClientBundleQuarter.setEventL(performanceClientBundleQuarter.getEventL() + performanceBundle.getAmount());break;
                    case "M": performanceClientBundleQuarter.setEventM(performanceClientBundleQuarter.getEventM() + performanceBundle.getAmount());break;
                    case "N": performanceClientBundleQuarter.setEventN(performanceClientBundleQuarter.getEventN() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleHourMap.containsKey(keyClientHour)) {
                    PerformanceClientBundleHour performanceClientBundleHour = new PerformanceClientBundleHour(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                    performanceClientBundleHourMap.put(keyClientHour, performanceClientBundleHour);
                }
                PerformanceClientBundleHour performanceClientBundleHour = performanceClientBundleHourMap.get(keyClientHour);
                switch (performanceBundle.getEvent().substring(1, 2)) {
                    case "A": performanceClientBundleHour.setEventA(performanceClientBundleHour.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceClientBundleHour.setEventB(performanceClientBundleHour.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceClientBundleHour.setEventC(performanceClientBundleHour.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceClientBundleHour.setEventD(performanceClientBundleHour.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceClientBundleHour.setEventE(performanceClientBundleHour.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceClientBundleHour.setEventF(performanceClientBundleHour.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceClientBundleHour.setEventG(performanceClientBundleHour.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceClientBundleHour.setEventH(performanceClientBundleHour.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceClientBundleHour.setEventI(performanceClientBundleHour.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceClientBundleHour.setEventJ(performanceClientBundleHour.getEventJ() + performanceBundle.getAmount());break;
                    case "K": performanceClientBundleHour.setEventK(performanceClientBundleHour.getEventK() + performanceBundle.getAmount());break;
                    case "L": performanceClientBundleHour.setEventL(performanceClientBundleHour.getEventL() + performanceBundle.getAmount());break;
                    case "M": performanceClientBundleHour.setEventM(performanceClientBundleHour.getEventM() + performanceBundle.getAmount());break;
                    case "N": performanceClientBundleHour.setEventN(performanceClientBundleHour.getEventN() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleDayMap.containsKey(keyClientDay)) {
                    PerformanceClientBundleDay performanceClientBundleDay = new PerformanceClientBundleDay(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                    performanceClientBundleDayMap.put(keyClientDay, performanceClientBundleDay);
                }
                PerformanceClientBundleDay performanceClientBundleDay = performanceClientBundleDayMap.get(keyClientDay);
                switch (performanceBundle.getEvent().substring(1, 2)) {
                    case "A": performanceClientBundleDay.setEventA(performanceClientBundleDay.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceClientBundleDay.setEventB(performanceClientBundleDay.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceClientBundleDay.setEventC(performanceClientBundleDay.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceClientBundleDay.setEventD(performanceClientBundleDay.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceClientBundleDay.setEventE(performanceClientBundleDay.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceClientBundleDay.setEventF(performanceClientBundleDay.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceClientBundleDay.setEventG(performanceClientBundleDay.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceClientBundleDay.setEventH(performanceClientBundleDay.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceClientBundleDay.setEventI(performanceClientBundleDay.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceClientBundleDay.setEventJ(performanceClientBundleDay.getEventJ() + performanceBundle.getAmount());break;
                    case "K": performanceClientBundleDay.setEventK(performanceClientBundleDay.getEventK() + performanceBundle.getAmount());break;
                    case "L": performanceClientBundleDay.setEventL(performanceClientBundleDay.getEventL() + performanceBundle.getAmount());break;
                    case "M": performanceClientBundleDay.setEventM(performanceClientBundleDay.getEventM() + performanceBundle.getAmount());break;
                    case "N": performanceClientBundleDay.setEventN(performanceClientBundleDay.getEventN() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleQuarterMap.containsKey(keyClientQuarter0)) {
                    PerformanceClientBundleQuarter performanceClientBundleQuarter0 = new PerformanceClientBundleQuarter(performanceBundle.getClientPort(), 0, performanceBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                    performanceClientBundleQuarterMap.put(keyClientQuarter0, performanceClientBundleQuarter0);
                }
                PerformanceClientBundleQuarter performanceClientBundleQuarter0 = performanceClientBundleQuarterMap.get(keyClientQuarter0);
                switch (performanceBundle.getEvent().substring(1, 2)) {
                    case "A": performanceClientBundleQuarter0.setEventA(performanceClientBundleQuarter0.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceClientBundleQuarter0.setEventB(performanceClientBundleQuarter0.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceClientBundleQuarter0.setEventC(performanceClientBundleQuarter0.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceClientBundleQuarter0.setEventD(performanceClientBundleQuarter0.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceClientBundleQuarter0.setEventE(performanceClientBundleQuarter0.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceClientBundleQuarter0.setEventF(performanceClientBundleQuarter0.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceClientBundleQuarter0.setEventG(performanceClientBundleQuarter0.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceClientBundleQuarter0.setEventH(performanceClientBundleQuarter0.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceClientBundleQuarter0.setEventI(performanceClientBundleQuarter0.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceClientBundleQuarter0.setEventJ(performanceClientBundleQuarter0.getEventJ() + performanceBundle.getAmount());break;
                    case "K": performanceClientBundleQuarter0.setEventK(performanceClientBundleQuarter0.getEventK() + performanceBundle.getAmount());break;
                    case "L": performanceClientBundleQuarter0.setEventL(performanceClientBundleQuarter0.getEventL() + performanceBundle.getAmount());break;
                    case "M": performanceClientBundleQuarter0.setEventM(performanceClientBundleQuarter0.getEventM() + performanceBundle.getAmount());break;
                    case "N": performanceClientBundleQuarter0.setEventN(performanceClientBundleQuarter0.getEventN() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleHourMap.containsKey(keyClientHour0)) {
                    PerformanceClientBundleHour performanceClientBundleHour0 = new PerformanceClientBundleHour(performanceBundle.getClientPort(), 0, performanceBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                    performanceClientBundleHourMap.put(keyClientHour0, performanceClientBundleHour0);
                }
                PerformanceClientBundleHour performanceClientBundleHour0 = performanceClientBundleHourMap.get(keyClientHour0);
                switch (performanceBundle.getEvent().substring(1, 2)) {
                    case "A": performanceClientBundleHour0.setEventA(performanceClientBundleHour0.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceClientBundleHour0.setEventB(performanceClientBundleHour0.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceClientBundleHour0.setEventC(performanceClientBundleHour0.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceClientBundleHour0.setEventD(performanceClientBundleHour0.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceClientBundleHour0.setEventE(performanceClientBundleHour0.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceClientBundleHour0.setEventF(performanceClientBundleHour0.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceClientBundleHour0.setEventG(performanceClientBundleHour0.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceClientBundleHour0.setEventH(performanceClientBundleHour0.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceClientBundleHour0.setEventI(performanceClientBundleHour0.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceClientBundleHour0.setEventJ(performanceClientBundleHour0.getEventJ() + performanceBundle.getAmount());break;
                    case "K": performanceClientBundleHour0.setEventK(performanceClientBundleHour0.getEventK() + performanceBundle.getAmount());break;
                    case "L": performanceClientBundleHour0.setEventL(performanceClientBundleHour0.getEventL() + performanceBundle.getAmount());break;
                    case "M": performanceClientBundleHour0.setEventM(performanceClientBundleHour0.getEventM() + performanceBundle.getAmount());break;
                    case "N": performanceClientBundleHour0.setEventN(performanceClientBundleHour0.getEventN() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleDayMap.containsKey(keyClientDay0)) {
                    PerformanceClientBundleDay performanceClientBundleDay0 = new PerformanceClientBundleDay(performanceBundle.getClientPort(), 0, performanceBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                    performanceClientBundleDayMap.put(keyClientDay0, performanceClientBundleDay0);
                }
                PerformanceClientBundleDay performanceClientBundleDay0 = performanceClientBundleDayMap.get(keyClientDay0);
                switch (performanceBundle.getEvent().substring(1, 2)) {
                    case "A": performanceClientBundleDay0.setEventA(performanceClientBundleDay0.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceClientBundleDay0.setEventB(performanceClientBundleDay0.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceClientBundleDay0.setEventC(performanceClientBundleDay0.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceClientBundleDay0.setEventD(performanceClientBundleDay0.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceClientBundleDay0.setEventE(performanceClientBundleDay0.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceClientBundleDay0.setEventF(performanceClientBundleDay0.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceClientBundleDay0.setEventG(performanceClientBundleDay0.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceClientBundleDay0.setEventH(performanceClientBundleDay0.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceClientBundleDay0.setEventI(performanceClientBundleDay0.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceClientBundleDay0.setEventJ(performanceClientBundleDay0.getEventJ() + performanceBundle.getAmount());break;
                    case "K": performanceClientBundleDay0.setEventK(performanceClientBundleDay0.getEventK() + performanceBundle.getAmount());break;
                    case "L": performanceClientBundleDay0.setEventL(performanceClientBundleDay0.getEventL() + performanceBundle.getAmount());break;
                    case "M": performanceClientBundleDay0.setEventM(performanceClientBundleDay0.getEventM() + performanceBundle.getAmount());break;
                    case "N": performanceClientBundleDay0.setEventN(performanceClientBundleDay0.getEventN() + performanceBundle.getAmount());break;
                    default: break;
                }
            }
            if ("ABCDEFGHIJ".indexOf(performanceBundle.getEvent().charAt(0)) >= 0) {
                String keyVendorQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleQuarterMap.containsKey(keyVendorQuarter)) {
                    PerformanceVendorBundleQuarter performanceVendorBundleQuarter = new PerformanceVendorBundleQuarter(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                    performanceVendorBundleQuarterMap.put(keyVendorQuarter, performanceVendorBundleQuarter);
                }
                PerformanceVendorBundleQuarter performanceVendorBundleQuarter = performanceVendorBundleQuarterMap.get(keyVendorQuarter);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "A": performanceVendorBundleQuarter.setEventA(performanceVendorBundleQuarter.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceVendorBundleQuarter.setEventB(performanceVendorBundleQuarter.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceVendorBundleQuarter.setEventC(performanceVendorBundleQuarter.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceVendorBundleQuarter.setEventD(performanceVendorBundleQuarter.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceVendorBundleQuarter.setEventE(performanceVendorBundleQuarter.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceVendorBundleQuarter.setEventF(performanceVendorBundleQuarter.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceVendorBundleQuarter.setEventG(performanceVendorBundleQuarter.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceVendorBundleQuarter.setEventH(performanceVendorBundleQuarter.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceVendorBundleQuarter.setEventI(performanceVendorBundleQuarter.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceVendorBundleQuarter.setEventJ(performanceVendorBundleQuarter.getEventJ() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleHourMap.containsKey(keyVendorHour)) {
                    PerformanceVendorBundleHour performanceVendorBundleHour = new PerformanceVendorBundleHour(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                    performanceVendorBundleHourMap.put(keyVendorHour, performanceVendorBundleHour);
                }
                PerformanceVendorBundleHour performanceVendorBundleHour = performanceVendorBundleHourMap.get(keyVendorHour);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "A": performanceVendorBundleHour.setEventA(performanceVendorBundleHour.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceVendorBundleHour.setEventB(performanceVendorBundleHour.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceVendorBundleHour.setEventC(performanceVendorBundleHour.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceVendorBundleHour.setEventD(performanceVendorBundleHour.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceVendorBundleHour.setEventE(performanceVendorBundleHour.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceVendorBundleHour.setEventF(performanceVendorBundleHour.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceVendorBundleHour.setEventG(performanceVendorBundleHour.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceVendorBundleHour.setEventH(performanceVendorBundleHour.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceVendorBundleHour.setEventI(performanceVendorBundleHour.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceVendorBundleHour.setEventJ(performanceVendorBundleHour.getEventJ() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleDayMap.containsKey(keyVendorDay)) {
                    PerformanceVendorBundleDay performanceVendorBundleDay = new PerformanceVendorBundleDay(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                    performanceVendorBundleDayMap.put(keyVendorDay, performanceVendorBundleDay);
                }
                PerformanceVendorBundleDay performanceVendorBundleDay = performanceVendorBundleDayMap.get(keyVendorDay);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "A": performanceVendorBundleDay.setEventA(performanceVendorBundleDay.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceVendorBundleDay.setEventB(performanceVendorBundleDay.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceVendorBundleDay.setEventC(performanceVendorBundleDay.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceVendorBundleDay.setEventD(performanceVendorBundleDay.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceVendorBundleDay.setEventE(performanceVendorBundleDay.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceVendorBundleDay.setEventF(performanceVendorBundleDay.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceVendorBundleDay.setEventG(performanceVendorBundleDay.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceVendorBundleDay.setEventH(performanceVendorBundleDay.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceVendorBundleDay.setEventI(performanceVendorBundleDay.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceVendorBundleDay.setEventJ(performanceVendorBundleDay.getEventJ() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleQuarterMap.containsKey(keyVendorQuarter0)) {
                    PerformanceVendorBundleQuarter performanceVendorBundleQuarter0 = new PerformanceVendorBundleQuarter(0, performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                    performanceVendorBundleQuarterMap.put(keyVendorQuarter0, performanceVendorBundleQuarter0);
                }
                PerformanceVendorBundleQuarter performanceVendorBundleQuarter0 = performanceVendorBundleQuarterMap.get(keyVendorQuarter0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "A": performanceVendorBundleQuarter0.setEventA(performanceVendorBundleQuarter0.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceVendorBundleQuarter0.setEventB(performanceVendorBundleQuarter0.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceVendorBundleQuarter0.setEventC(performanceVendorBundleQuarter0.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceVendorBundleQuarter0.setEventD(performanceVendorBundleQuarter0.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceVendorBundleQuarter0.setEventE(performanceVendorBundleQuarter0.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceVendorBundleQuarter0.setEventF(performanceVendorBundleQuarter0.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceVendorBundleQuarter0.setEventG(performanceVendorBundleQuarter0.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceVendorBundleQuarter0.setEventH(performanceVendorBundleQuarter0.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceVendorBundleQuarter0.setEventI(performanceVendorBundleQuarter0.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceVendorBundleQuarter0.setEventJ(performanceVendorBundleQuarter0.getEventJ() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleHourMap.containsKey(keyVendorHour0)) {
                    PerformanceVendorBundleHour performanceVendorBundleHour0 = new PerformanceVendorBundleHour(0, performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                    performanceVendorBundleHourMap.put(keyVendorHour0, performanceVendorBundleHour0);
                }
                PerformanceVendorBundleHour performanceVendorBundleHour0 = performanceVendorBundleHourMap.get(keyVendorHour0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "A": performanceVendorBundleHour0.setEventA(performanceVendorBundleHour0.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceVendorBundleHour0.setEventB(performanceVendorBundleHour0.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceVendorBundleHour0.setEventC(performanceVendorBundleHour0.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceVendorBundleHour0.setEventD(performanceVendorBundleHour0.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceVendorBundleHour0.setEventE(performanceVendorBundleHour0.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceVendorBundleHour0.setEventF(performanceVendorBundleHour0.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceVendorBundleHour0.setEventG(performanceVendorBundleHour0.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceVendorBundleHour0.setEventH(performanceVendorBundleHour0.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceVendorBundleHour0.setEventI(performanceVendorBundleHour0.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceVendorBundleHour0.setEventJ(performanceVendorBundleHour0.getEventJ() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleDayMap.containsKey(keyVendorDay0)) {
                    PerformanceVendorBundleDay performanceVendorBundleDay0 = new PerformanceVendorBundleDay(0, performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                    performanceVendorBundleDayMap.put(keyVendorDay0, performanceVendorBundleDay0);
                }
                PerformanceVendorBundleDay performanceVendorBundleDay0 = performanceVendorBundleDayMap.get(keyVendorDay0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "A": performanceVendorBundleDay0.setEventA(performanceVendorBundleDay0.getEventA() + performanceBundle.getAmount());break;
                    case "B": performanceVendorBundleDay0.setEventB(performanceVendorBundleDay0.getEventB() + performanceBundle.getAmount());break;
                    case "C": performanceVendorBundleDay0.setEventC(performanceVendorBundleDay0.getEventC() + performanceBundle.getAmount());break;
                    case "D": performanceVendorBundleDay0.setEventD(performanceVendorBundleDay0.getEventD() + performanceBundle.getAmount());break;
                    case "E": performanceVendorBundleDay0.setEventE(performanceVendorBundleDay0.getEventE() + performanceBundle.getAmount());break;
                    case "F": performanceVendorBundleDay0.setEventF(performanceVendorBundleDay0.getEventF() + performanceBundle.getAmount());break;
                    case "G": performanceVendorBundleDay0.setEventG(performanceVendorBundleDay0.getEventG() + performanceBundle.getAmount());break;
                    case "H": performanceVendorBundleDay0.setEventH(performanceVendorBundleDay0.getEventH() + performanceBundle.getAmount());break;
                    case "I": performanceVendorBundleDay0.setEventI(performanceVendorBundleDay0.getEventI() + performanceBundle.getAmount());break;
                    case "J": performanceVendorBundleDay0.setEventJ(performanceVendorBundleDay0.getEventJ() + performanceBundle.getAmount());break;
                    default: break;
                }
            }
            if ("ab".indexOf(performanceBundle.getEvent().charAt(0)) >= 0) {
                String keyClientQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleQuarterMap.containsKey(keyClientQuarter)) {
                    PerformanceClientBundleQuarter performanceClientBundleQuarter = new PerformanceClientBundleQuarter(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                    performanceClientBundleQuarterMap.put(keyClientQuarter, performanceClientBundleQuarter);
                }
                PerformanceClientBundleQuarter performanceClientBundleQuarter = performanceClientBundleQuarterMap.get(keyClientQuarter);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceClientBundleQuarter.setImpression(performanceClientBundleQuarter.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceClientBundleQuarter.setClick(performanceClientBundleQuarter.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleHourMap.containsKey(keyClientHour)) {
                    PerformanceClientBundleHour performanceClientBundleHour = new PerformanceClientBundleHour(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                    performanceClientBundleHourMap.put(keyClientHour, performanceClientBundleHour);
                }
                PerformanceClientBundleHour performanceClientBundleHour = performanceClientBundleHourMap.get(keyClientHour);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceClientBundleHour.setImpression(performanceClientBundleHour.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceClientBundleHour.setClick(performanceClientBundleHour.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleDayMap.containsKey(keyClientDay)) {
                    PerformanceClientBundleDay performanceClientBundleDay = new PerformanceClientBundleDay(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                    performanceClientBundleDayMap.put(keyClientDay, performanceClientBundleDay);
                }
                PerformanceClientBundleDay performanceClientBundleDay = performanceClientBundleDayMap.get(keyClientDay);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceClientBundleDay.setImpression(performanceClientBundleDay.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceClientBundleDay.setClick(performanceClientBundleDay.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleQuarterMap.containsKey(keyVendorQuarter)) {
                    PerformanceVendorBundleQuarter performanceVendorBundleQuarter = new PerformanceVendorBundleQuarter(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                    performanceVendorBundleQuarterMap.put(keyVendorQuarter, performanceVendorBundleQuarter);
                }
                PerformanceVendorBundleQuarter performanceVendorBundleQuarter = performanceVendorBundleQuarterMap.get(keyVendorQuarter);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceVendorBundleQuarter.setImpression(performanceVendorBundleQuarter.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceVendorBundleQuarter.setClick(performanceVendorBundleQuarter.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleHourMap.containsKey(keyVendorHour)) {
                    PerformanceVendorBundleHour performanceVendorBundleHour = new PerformanceVendorBundleHour(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                    performanceVendorBundleHourMap.put(keyVendorHour, performanceVendorBundleHour);
                }
                PerformanceVendorBundleHour performanceVendorBundleHour = performanceVendorBundleHourMap.get(keyVendorHour);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceVendorBundleHour.setImpression(performanceVendorBundleHour.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceVendorBundleHour.setClick(performanceVendorBundleHour.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleDayMap.containsKey(keyVendorDay)) {
                    PerformanceVendorBundleDay performanceVendorBundleDay = new PerformanceVendorBundleDay(performanceBundle.getClientPort(), performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                    performanceVendorBundleDayMap.put(keyVendorDay, performanceVendorBundleDay);
                }
                PerformanceVendorBundleDay performanceVendorBundleDay = performanceVendorBundleDayMap.get(keyVendorDay);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceVendorBundleDay.setImpression(performanceVendorBundleDay.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceVendorBundleDay.setClick(performanceVendorBundleDay.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleQuarterMap.containsKey(keyClientQuarter0)) {
                    PerformanceClientBundleQuarter performanceClientBundleQuarter0 = new PerformanceClientBundleQuarter(performanceBundle.getClientPort(), 0, performanceBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                    performanceClientBundleQuarterMap.put(keyClientQuarter0, performanceClientBundleQuarter0);
                }
                PerformanceClientBundleQuarter performanceClientBundleQuarter0 = performanceClientBundleQuarterMap.get(keyClientQuarter0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceClientBundleQuarter0.setImpression(performanceClientBundleQuarter0.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceClientBundleQuarter0.setClick(performanceClientBundleQuarter0.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleHourMap.containsKey(keyClientHour0)) {
                    PerformanceClientBundleHour performanceClientBundleHour0 = new PerformanceClientBundleHour(performanceBundle.getClientPort(), 0, performanceBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                    performanceClientBundleHourMap.put(keyClientHour0, performanceClientBundleHour0);
                }
                PerformanceClientBundleHour performanceClientBundleHour0 = performanceClientBundleHourMap.get(keyClientHour0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceClientBundleHour0.setImpression(performanceClientBundleHour0.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceClientBundleHour0.setClick(performanceClientBundleHour0.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyClientDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performanceBundle.getClientPort() + "|" + performanceBundle.getBundle();
                if (!performanceClientBundleDayMap.containsKey(keyClientDay0)) {
                    PerformanceClientBundleDay performanceClientBundleDay0 = new PerformanceClientBundleDay(performanceBundle.getClientPort(), 0, performanceBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                    performanceClientBundleDayMap.put(keyClientDay0, performanceClientBundleDay0);
                }
                PerformanceClientBundleDay performanceClientBundleDay0 = performanceClientBundleDayMap.get(keyClientDay0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceClientBundleDay0.setImpression(performanceClientBundleDay0.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceClientBundleDay0.setClick(performanceClientBundleDay0.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleQuarterMap.containsKey(keyVendorQuarter0)) {
                    PerformanceVendorBundleQuarter performanceVendorBundleQuarter0 = new PerformanceVendorBundleQuarter(0, performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                    performanceVendorBundleQuarterMap.put(keyVendorQuarter0, performanceVendorBundleQuarter0);
                }
                PerformanceVendorBundleQuarter performanceVendorBundleQuarter0 = performanceVendorBundleQuarterMap.get(keyVendorQuarter0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceVendorBundleQuarter0.setImpression(performanceVendorBundleQuarter0.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceVendorBundleQuarter0.setClick(performanceVendorBundleQuarter0.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleHourMap.containsKey(keyVendorHour0)) {
                    PerformanceVendorBundleHour performanceVendorBundleHour0 = new PerformanceVendorBundleHour(0, performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                    performanceVendorBundleHourMap.put(keyVendorHour0, performanceVendorBundleHour0);
                }
                PerformanceVendorBundleHour performanceVendorBundleHour0 = performanceVendorBundleHourMap.get(keyVendorHour0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceVendorBundleHour0.setImpression(performanceVendorBundleHour0.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceVendorBundleHour0.setClick(performanceVendorBundleHour0.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }

                String keyVendorDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + performanceBundle.getVendorPort() + "|" + performanceBundle.getBundle();
                if (!performanceVendorBundleDayMap.containsKey(keyVendorDay0)) {
                    PerformanceVendorBundleDay performanceVendorBundleDay0 = new PerformanceVendorBundleDay(0, performanceBundle.getVendorPort(), performanceBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                    performanceVendorBundleDayMap.put(keyVendorDay0, performanceVendorBundleDay0);
                }
                PerformanceVendorBundleDay performanceVendorBundleDay0 = performanceVendorBundleDayMap.get(keyVendorDay0);
                switch (performanceBundle.getEvent().substring(0, 1)) {
                    case "a": performanceVendorBundleDay0.setImpression(performanceVendorBundleDay0.getImpression() + performanceBundle.getAmount());break;
                    case "b": performanceVendorBundleDay0.setClick(performanceVendorBundleDay0.getClick() + performanceBundle.getAmount());break;
                    default: break;
                }
            }
        }
        for (FinanceBundle financeBundle : financeBundleList) {
            ZonedDateTime time = financeBundle.getTime().toInstant().atZone(ZoneId.of(timezone));
            ZonedDateTime timeQuarter = time.withMinute(time.getMinute() / 15 * 15).withSecond(0).withNano(0);
            ZonedDateTime timeHour = time.withMinute(0).withSecond(0).withNano(0);
            ZonedDateTime timeDay = time.withHour(0).withMinute(0).withSecond(0).withNano(0);
            String existingQuarterKey = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();

            if (incremental && (existingPerformanceClientBundleQuarterKeys.contains(existingQuarterKey) || existingPerformanceVendorBundleQuarterKeys.contains(existingQuarterKey))) {
                continue;
            }

            String keyClientQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceClientBundleQuarterMap.containsKey(keyClientQuarter)) {
                PerformanceClientBundleQuarter performanceClientBundleQuarter = new PerformanceClientBundleQuarter(financeBundle.getClientPort(), financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                performanceClientBundleQuarterMap.put(keyClientQuarter, performanceClientBundleQuarter);
            }
            PerformanceClientBundleQuarter performanceClientBundleQuarter = performanceClientBundleQuarterMap.get(keyClientQuarter);
            performanceClientBundleQuarter.setIncome(performanceClientBundleQuarter.getIncome() + financeBundle.getIncome());
            performanceClientBundleQuarter.setOutcomeUpstream(performanceClientBundleQuarter.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceClientBundleQuarter.setOutcomeRebate(performanceClientBundleQuarter.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceClientBundleQuarter.setOutcomeDownstream(performanceClientBundleQuarter.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyClientHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceClientBundleHourMap.containsKey(keyClientHour)) {
                PerformanceClientBundleHour performanceClientBundleHour = new PerformanceClientBundleHour(financeBundle.getClientPort(), financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                performanceClientBundleHourMap.put(keyClientHour, performanceClientBundleHour);
            }
            PerformanceClientBundleHour performanceClientBundleHour = performanceClientBundleHourMap.get(keyClientHour);
            performanceClientBundleHour.setIncome(performanceClientBundleHour.getIncome() + financeBundle.getIncome());
            performanceClientBundleHour.setOutcomeUpstream(performanceClientBundleHour.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceClientBundleHour.setOutcomeRebate(performanceClientBundleHour.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceClientBundleHour.setOutcomeDownstream(performanceClientBundleHour.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyClientDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceClientBundleDayMap.containsKey(keyClientDay)) {
                PerformanceClientBundleDay performanceClientBundleDay = new PerformanceClientBundleDay(financeBundle.getClientPort(), financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                performanceClientBundleDayMap.put(keyClientDay, performanceClientBundleDay);
            }
            PerformanceClientBundleDay performanceClientBundleDay = performanceClientBundleDayMap.get(keyClientDay);
            performanceClientBundleDay.setIncome(performanceClientBundleDay.getIncome() + financeBundle.getIncome());
            performanceClientBundleDay.setOutcomeUpstream(performanceClientBundleDay.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceClientBundleDay.setOutcomeRebate(performanceClientBundleDay.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceClientBundleDay.setOutcomeDownstream(performanceClientBundleDay.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyVendorQuarter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceVendorBundleQuarterMap.containsKey(keyVendorQuarter)) {
                PerformanceVendorBundleQuarter performanceVendorBundleQuarter = new PerformanceVendorBundleQuarter(financeBundle.getClientPort(), financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                performanceVendorBundleQuarterMap.put(keyVendorQuarter, performanceVendorBundleQuarter);
            }
            PerformanceVendorBundleQuarter performanceVendorBundleQuarter = performanceVendorBundleQuarterMap.get(keyVendorQuarter);
            performanceVendorBundleQuarter.setIncome(performanceVendorBundleQuarter.getIncome() + financeBundle.getIncome());
            performanceVendorBundleQuarter.setOutcomeUpstream(performanceVendorBundleQuarter.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceVendorBundleQuarter.setOutcomeRebate(performanceVendorBundleQuarter.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceVendorBundleQuarter.setOutcomeDownstream(performanceVendorBundleQuarter.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyVendorHour = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceVendorBundleHourMap.containsKey(keyVendorHour)) {
                PerformanceVendorBundleHour performanceVendorBundleHour = new PerformanceVendorBundleHour(financeBundle.getClientPort(), financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                performanceVendorBundleHourMap.put(keyVendorHour, performanceVendorBundleHour);
            }
            PerformanceVendorBundleHour performanceVendorBundleHour = performanceVendorBundleHourMap.get(keyVendorHour);
            performanceVendorBundleHour.setIncome(performanceVendorBundleHour.getIncome() + financeBundle.getIncome());
            performanceVendorBundleHour.setOutcomeUpstream(performanceVendorBundleHour.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceVendorBundleHour.setOutcomeRebate(performanceVendorBundleHour.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceVendorBundleHour.setOutcomeDownstream(performanceVendorBundleHour.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyVendorDay = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceVendorBundleDayMap.containsKey(keyVendorDay)) {
                PerformanceVendorBundleDay performanceVendorBundleDay = new PerformanceVendorBundleDay(financeBundle.getClientPort(), financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                performanceVendorBundleDayMap.put(keyVendorDay, performanceVendorBundleDay);
            }
            PerformanceVendorBundleDay performanceVendorBundleDay = performanceVendorBundleDayMap.get(keyVendorDay);
            performanceVendorBundleDay.setIncome(performanceVendorBundleDay.getIncome() + financeBundle.getIncome());
            performanceVendorBundleDay.setOutcomeUpstream(performanceVendorBundleDay.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceVendorBundleDay.setOutcomeRebate(performanceVendorBundleDay.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceVendorBundleDay.setOutcomeDownstream(performanceVendorBundleDay.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyClientQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getBundle();
            if (!performanceClientBundleQuarterMap.containsKey(keyClientQuarter0)) {
                PerformanceClientBundleQuarter performanceClientBundleQuarter0 = new PerformanceClientBundleQuarter(financeBundle.getClientPort(), 0, financeBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                performanceClientBundleQuarterMap.put(keyClientQuarter0, performanceClientBundleQuarter0);
            }
            PerformanceClientBundleQuarter performanceClientBundleQuarter0 = performanceClientBundleQuarterMap.get(keyClientQuarter0);
            performanceClientBundleQuarter0.setIncome(performanceClientBundleQuarter0.getIncome() + financeBundle.getIncome());
            performanceClientBundleQuarter0.setOutcomeUpstream(performanceClientBundleQuarter0.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceClientBundleQuarter0.setOutcomeRebate(performanceClientBundleQuarter0.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceClientBundleQuarter0.setOutcomeDownstream(performanceClientBundleQuarter0.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyClientHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getBundle();
            if (!performanceClientBundleHourMap.containsKey(keyClientHour0)) {
                PerformanceClientBundleHour performanceClientBundleHour0 = new PerformanceClientBundleHour(financeBundle.getClientPort(), 0, financeBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                performanceClientBundleHourMap.put(keyClientHour0, performanceClientBundleHour0);
            }
            PerformanceClientBundleHour performanceClientBundleHour0 = performanceClientBundleHourMap.get(keyClientHour0);
            performanceClientBundleHour0.setIncome(performanceClientBundleHour0.getIncome() + financeBundle.getIncome());
            performanceClientBundleHour0.setOutcomeUpstream(performanceClientBundleHour0.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceClientBundleHour0.setOutcomeRebate(performanceClientBundleHour0.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceClientBundleHour0.setOutcomeDownstream(performanceClientBundleHour0.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyClientDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + financeBundle.getClientPort() + "|" + financeBundle.getBundle();
            if (!performanceClientBundleDayMap.containsKey(keyClientDay0)) {
                PerformanceClientBundleDay performanceClientBundleDay0 = new PerformanceClientBundleDay(financeBundle.getClientPort(), 0, financeBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                performanceClientBundleDayMap.put(keyClientDay0, performanceClientBundleDay0);
            }
            PerformanceClientBundleDay performanceClientBundleDay0 = performanceClientBundleDayMap.get(keyClientDay0);
            performanceClientBundleDay0.setIncome(performanceClientBundleDay0.getIncome() + financeBundle.getIncome());
            performanceClientBundleDay0.setOutcomeUpstream(performanceClientBundleDay0.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceClientBundleDay0.setOutcomeRebate(performanceClientBundleDay0.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceClientBundleDay0.setOutcomeDownstream(performanceClientBundleDay0.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyVendorQuarter0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeQuarter) + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceVendorBundleQuarterMap.containsKey(keyVendorQuarter0)) {
                PerformanceVendorBundleQuarter performanceVendorBundleQuarter0 = new PerformanceVendorBundleQuarter(0, financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeQuarter.toInstant()));
                performanceVendorBundleQuarterMap.put(keyVendorQuarter0, performanceVendorBundleQuarter0);
            }
            PerformanceVendorBundleQuarter performanceVendorBundleQuarter0 = performanceVendorBundleQuarterMap.get(keyVendorQuarter0);
            performanceVendorBundleQuarter0.setIncome(performanceVendorBundleQuarter0.getIncome() + financeBundle.getIncome());
            performanceVendorBundleQuarter0.setOutcomeUpstream(performanceVendorBundleQuarter0.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceVendorBundleQuarter0.setOutcomeRebate(performanceVendorBundleQuarter0.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceVendorBundleQuarter0.setOutcomeDownstream(performanceVendorBundleQuarter0.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyVendorHour0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeHour) + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceVendorBundleHourMap.containsKey(keyVendorHour0)) {
                PerformanceVendorBundleHour performanceVendorBundleHour0 = new PerformanceVendorBundleHour(0, financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeHour.toInstant()));
                performanceVendorBundleHourMap.put(keyVendorHour0, performanceVendorBundleHour0);
            }
            PerformanceVendorBundleHour performanceVendorBundleHour0 = performanceVendorBundleHourMap.get(keyVendorHour0);
            performanceVendorBundleHour0.setIncome(performanceVendorBundleHour0.getIncome() + financeBundle.getIncome());
            performanceVendorBundleHour0.setOutcomeUpstream(performanceVendorBundleHour0.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceVendorBundleHour0.setOutcomeRebate(performanceVendorBundleHour0.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceVendorBundleHour0.setOutcomeDownstream(performanceVendorBundleHour0.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());

            String keyVendorDay0 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timeDay) + "|" + financeBundle.getVendorPort() + "|" + financeBundle.getBundle();
            if (!performanceVendorBundleDayMap.containsKey(keyVendorDay0)) {
                PerformanceVendorBundleDay performanceVendorBundleDay0 = new PerformanceVendorBundleDay(0, financeBundle.getVendorPort(), financeBundle.getBundle(), Timestamp.from(timeDay.toInstant()));
                performanceVendorBundleDayMap.put(keyVendorDay0, performanceVendorBundleDay0);
            }
            PerformanceVendorBundleDay performanceVendorBundleDay0 = performanceVendorBundleDayMap.get(keyVendorDay0);
            performanceVendorBundleDay0.setIncome(performanceVendorBundleDay0.getIncome() + financeBundle.getIncome());
            performanceVendorBundleDay0.setOutcomeUpstream(performanceVendorBundleDay0.getOutcomeUpstream() + (financeBundle.getOutcomeUpstream() == null ? 0 : financeBundle.getOutcomeUpstream()));
            performanceVendorBundleDay0.setOutcomeRebate(performanceVendorBundleDay0.getOutcomeRebate() + (financeBundle.getOutcomeRebate() == null ? 0 : financeBundle.getOutcomeRebate()));
            performanceVendorBundleDay0.setOutcomeDownstream(performanceVendorBundleDay0.getOutcomeDownstream() + financeBundle.getOutcomeDownstream());
        }

        performanceClientBundleQuarterRepository.saveAll(performanceClientBundleQuarterMap.values());
        performanceClientBundleHourRepository.saveAll(performanceClientBundleHourMap.values());
        performanceClientBundleDayRepository.saveAll(performanceClientBundleDayMap.values());
        performanceVendorBundleQuarterRepository.saveAll(performanceVendorBundleQuarterMap.values());
        performanceVendorBundleHourRepository.saveAll(performanceVendorBundleHourMap.values());
        performanceVendorBundleDayRepository.saveAll(performanceVendorBundleDayMap.values());
    }

}
