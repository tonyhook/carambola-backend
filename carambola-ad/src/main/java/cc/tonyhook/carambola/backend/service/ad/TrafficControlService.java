package cc.tonyhook.carambola.backend.service.ad;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientBundleDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientBundleHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.TrafficControlRepository;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientHour;
import cc.tonyhook.carambola.backend.entity.ad.TrafficControl;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.shared.Query;
import io.lettuce.core.api.StatefulConnection;
import jakarta.transaction.Transactional;

@Service
public class TrafficControlService {

    private final TrafficControlRepository trafficControlRepository;
    private final PerformanceClientHourRepository performanceClientHourRepository;
    private final PerformanceClientDayRepository performanceClientDayRepository;
    private final PerformanceClientBundleHourRepository performanceClientBundleHourRepository;
    private final PerformanceClientBundleDayRepository performanceClientBundleDayRepository;

    private final PartnerService partnerService;

    public TrafficControlService(
            TrafficControlRepository trafficControlRepository,
            PerformanceClientHourRepository performanceClientHourRepository,
            PerformanceClientDayRepository performanceClientDayRepository,
            PerformanceClientBundleHourRepository performanceClientBundleHourRepository,
            PerformanceClientBundleDayRepository performanceClientBundleDayRepository,
            PartnerService partnerService
    ) {
        this.trafficControlRepository = trafficControlRepository;
        this.performanceClientHourRepository = performanceClientHourRepository;
        this.performanceClientDayRepository = performanceClientDayRepository;
        this.performanceClientBundleHourRepository = performanceClientBundleHourRepository;
        this.performanceClientBundleDayRepository = performanceClientBundleDayRepository;
        this.partnerService = partnerService;
    }

    @Value("${app.trafficcontrol-repository}")
    private String trafficControlCacheRepository;

    private RedisConnectionFactory redisConnectionFactory = null;

    public RedisConnectionFactory redisConnectionFactory() {
        if (redisConnectionFactory != null) {
            return redisConnectionFactory;
        }

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(trafficControlCacheRepository);

        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofSeconds(60))
            .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
        factory.afterPropertiesSet();
        this.redisConnectionFactory = factory;

        return redisConnectionFactory;
    }

    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        GenericToStringSerializer<String> genericToStringSerializer = new GenericToStringSerializer<String>(String.class);
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(genericToStringSerializer);
        template.afterPropertiesSet();

        return template;
    }

    public List<TrafficControl> getTrafficControlList(Authentication authentication, Query query) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());

        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());

        if (query.filter.get("clientPort") != null && query.filter.get("clientPort").contains("-1")) {
            clientPortIdList.add(-1);
        }
        if (query.filter.get("vendorPort") != null && query.filter.get("vendorPort").contains("-1")) {
            vendorPortIdList.add(-1);
        }

        List<TrafficControl> trafficControlList = trafficControlRepository.findByClientPortInAndVendorPortIn(clientPortIdList, vendorPortIdList);

        return trafficControlList;
    }

    public List<TrafficControl> getTrafficControlList(Authentication authentication, Integer clientPortId, Integer vendorPortId) {
        List<TrafficControl> trafficControlList = trafficControlRepository.findByClientPortAndVendorPort(clientPortId, vendorPortId);

        return trafficControlList;
    }

    public TrafficControl getTrafficControl(Integer id) {
        TrafficControl trafficControl = trafficControlRepository.findById(id).orElse(null);

        if (trafficControl != null) {
            return trafficControl;
        } else {
            return null;
        }
    }

    public TrafficControl addTrafficControl(TrafficControl newTrafficControl) {
        if (newTrafficControl != null) {
            TrafficControl updatedTrafficControl = trafficControlRepository.save(newTrafficControl);

            Timestamp time = new Timestamp(System.currentTimeMillis());

            if (updatedTrafficControl.getIndicator() == TrafficControl.TC_INDICATOR_REQUEST
                && updatedTrafficControl.getPeriod() == TrafficControl.TC_PERIOD_DAY) {
                Integer day = time.toInstant().atZone(ZoneId.of("GMT+08:00")).getDayOfMonth();
                Timestamp startOfDay = new Timestamp(time.toInstant().atZone(ZoneId.of("GMT+08:00")).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());

                String key = String.format("QRD%02d:%d:%d:%s",
                    day,
                    updatedTrafficControl.getClientPort(),
                    updatedTrafficControl.getVendorPort(),
                    StringUtils.replace(updatedTrafficControl.getBundle(), ":", "_"));
                Long value = 0L;

                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() == -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfDay, startOfDay);
                        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                            value += performanceClientDay.getEventC() + performanceClientDay.getEventD() + performanceClientDay.getEventE() + performanceClientDay.getEventF() + performanceClientDay.getEventK()
                                + performanceClientDay.getEventO();
                        }
                    } else {
                        List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfDay, startOfDay);
                        for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                            value += performanceClientBundleDay.getEventC() + performanceClientBundleDay.getEventD() + performanceClientBundleDay.getEventE() + performanceClientBundleDay.getEventF() + performanceClientBundleDay.getEventK()
                                + performanceClientBundleDay.getEventO();
                        }
                    }
                }
                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() != -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    List<Integer> vendorPortList = List.of(updatedTrafficControl.getVendorPort());
                    List<String> bundleList = List.of(updatedTrafficControl.getBundle());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortList, vendorPortList, startOfDay, startOfDay);
                        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                            value += performanceClientDay.getEventC() + performanceClientDay.getEventD() + performanceClientDay.getEventE() + performanceClientDay.getEventF() + performanceClientDay.getEventK()
                                + performanceClientDay.getEventO();
                        }
                    } else {
                        List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(clientPortList, vendorPortList, bundleList, startOfDay, startOfDay);
                        for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                            value += performanceClientBundleDay.getEventC() + performanceClientBundleDay.getEventD() + performanceClientBundleDay.getEventE() + performanceClientBundleDay.getEventF() + performanceClientBundleDay.getEventK()
                                + performanceClientBundleDay.getEventO();
                        }
                    }
                }

                Long expire = 86400L + 60;

                RedisTemplate<String, String> redisTemplate = redisTemplate();
                redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(expire));
            }
            if (updatedTrafficControl.getIndicator() == TrafficControl.TC_INDICATOR_REQUEST
                && updatedTrafficControl.getPeriod() == TrafficControl.TC_PERIOD_HOUR) {
                Integer hour = time.toInstant().atZone(ZoneId.of("GMT+00:00")).getHour();
                Timestamp startOfHour = new Timestamp(time.toInstant().atZone(ZoneId.of("GMT+00:00")).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());

                String key = String.format("QRH%02d:%d:%d:%s",
                    hour,
                    updatedTrafficControl.getClientPort(),
                    updatedTrafficControl.getVendorPort(),
                    StringUtils.replace(updatedTrafficControl.getBundle(), ":", "_"));
                Long value = 0L;

                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() == -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfHour, startOfHour);
                        for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                            value += performanceClientHour.getEventC() + performanceClientHour.getEventD() + performanceClientHour.getEventE() + performanceClientHour.getEventF() + performanceClientHour.getEventK()
                                + performanceClientHour.getEventO();
                        }
                    } else {
                        List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfHour, startOfHour);
                        for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                            value += performanceClientBundleHour.getEventC() + performanceClientBundleHour.getEventD() + performanceClientBundleHour.getEventE() + performanceClientBundleHour.getEventF() + performanceClientBundleHour.getEventK()
                                + performanceClientBundleHour.getEventO();
                        }
                    }
                }
                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() != -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    List<Integer> vendorPortList = List.of(updatedTrafficControl.getVendorPort());
                    List<String> bundleList = List.of(updatedTrafficControl.getBundle());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortList, vendorPortList, startOfHour, startOfHour);
                        for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                            value += performanceClientHour.getEventC() + performanceClientHour.getEventD() + performanceClientHour.getEventE() + performanceClientHour.getEventF() + performanceClientHour.getEventK()
                                + performanceClientHour.getEventO();
                        }
                    } else {
                        List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(clientPortList, vendorPortList, bundleList, startOfHour, startOfHour);
                        for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                            value += performanceClientBundleHour.getEventC() + performanceClientBundleHour.getEventD() + performanceClientBundleHour.getEventE() + performanceClientBundleHour.getEventF() + performanceClientBundleHour.getEventK()
                                + performanceClientBundleHour.getEventO();
                        }
                    }
                }

                Long expire = 3600L + 60;

                RedisTemplate<String, String> redisTemplate = redisTemplate();
                redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(expire));
            }

            if (updatedTrafficControl.getIndicator() == TrafficControl.TC_INDICATOR_COST
                && updatedTrafficControl.getPeriod() == TrafficControl.TC_PERIOD_DAY) {
                Integer day = time.toInstant().atZone(ZoneId.of("GMT+08:00")).getDayOfMonth();
                Timestamp startOfDay = new Timestamp(time.toInstant().atZone(ZoneId.of("GMT+08:00")).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());

                String key = String.format("QCD%02d:%d:%d:%s",
                    day,
                    updatedTrafficControl.getClientPort(),
                    updatedTrafficControl.getVendorPort(),
                    StringUtils.replace(updatedTrafficControl.getBundle(), ":", "_"));
                Long value = 0L;

                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() == -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfDay, startOfDay);
                        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                            value += performanceClientDay.getIncome();
                        }
                    } else {
                        List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfDay, startOfDay);
                        for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                            value += performanceClientBundleDay.getIncome();
                        }
                    }
                }
                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() != -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    List<Integer> vendorPortList = List.of(updatedTrafficControl.getVendorPort());
                    List<String> bundleList = List.of(updatedTrafficControl.getBundle());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortList, vendorPortList, startOfDay, startOfDay);
                        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                            value += performanceClientDay.getIncome();
                        }
                    } else {
                        List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(clientPortList, vendorPortList, bundleList, startOfDay, startOfDay);
                        for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                            value += performanceClientBundleDay.getIncome();
                        }
                    }
                }

                Long expire = 86400L + 60;

                RedisTemplate<String, String> redisTemplate = redisTemplate();
                redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(expire));
            }
            if (updatedTrafficControl.getIndicator() == TrafficControl.TC_INDICATOR_COST
                && updatedTrafficControl.getPeriod() == TrafficControl.TC_PERIOD_HOUR) {
                Integer hour = time.toInstant().atZone(ZoneId.of("GMT+00:00")).getHour();
                Timestamp startOfHour = new Timestamp(time.toInstant().atZone(ZoneId.of("GMT+00:00")).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());

                String key = String.format("QCH%02d:%d:%d:%s",
                    hour,
                    updatedTrafficControl.getClientPort(),
                    updatedTrafficControl.getVendorPort(),
                    StringUtils.replace(updatedTrafficControl.getBundle(), ":", "_"));
                Long value = 0L;

                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() == -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfHour, startOfHour);
                        for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                            value += performanceClientHour.getIncome();
                        }
                    } else {
                        List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfHour, startOfHour);
                        for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                            value += performanceClientBundleHour.getIncome();
                        }
                    }
                }
                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() != -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    List<Integer> vendorPortList = List.of(updatedTrafficControl.getVendorPort());
                    List<String> bundleList = List.of(updatedTrafficControl.getBundle());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortList, vendorPortList, startOfHour, startOfHour);
                        for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                            value += performanceClientHour.getIncome();
                        }
                    } else {
                        List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(clientPortList, vendorPortList, bundleList, startOfHour, startOfHour);
                        for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                            value += performanceClientBundleHour.getIncome();
                        }
                    }
                }

                Long expire = 3600L + 60;

                RedisTemplate<String, String> redisTemplate = redisTemplate();
                redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(expire));
            }

            return updatedTrafficControl;
        } else {
            return null;
        }
    }

    public void updateTrafficControl(Integer id, TrafficControl newTrafficControl) {
        if (newTrafficControl != null) {
            TrafficControl updatedTrafficControl = trafficControlRepository.save(newTrafficControl);

            Timestamp time = new Timestamp(System.currentTimeMillis());

            if (updatedTrafficControl.getIndicator() == TrafficControl.TC_INDICATOR_REQUEST
                && updatedTrafficControl.getPeriod() == TrafficControl.TC_PERIOD_DAY) {
                Integer day = time.toInstant().atZone(ZoneId.of("GMT+08:00")).getDayOfMonth();
                Timestamp startOfDay = new Timestamp(time.toInstant().atZone(ZoneId.of("GMT+08:00")).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());

                String key = String.format("QRD%02d:%d:%d:%s",
                    day,
                    updatedTrafficControl.getClientPort(),
                    updatedTrafficControl.getVendorPort(),
                    StringUtils.replace(updatedTrafficControl.getBundle(), ":", "_"));
                Long value = 0L;

                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() == -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfDay, startOfDay);
                        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                            value += performanceClientDay.getEventC() + performanceClientDay.getEventD() + performanceClientDay.getEventE() + performanceClientDay.getEventF() + performanceClientDay.getEventK()
                                + performanceClientDay.getEventO();
                        }
                    } else {
                        List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfDay, startOfDay);
                        for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                            value += performanceClientBundleDay.getEventC() + performanceClientBundleDay.getEventD() + performanceClientBundleDay.getEventE() + performanceClientBundleDay.getEventF() + performanceClientBundleDay.getEventK()
                                + performanceClientBundleDay.getEventO();
                        }
                    }
                }
                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() != -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    List<Integer> vendorPortList = List.of(updatedTrafficControl.getVendorPort());
                    List<String> bundleList = List.of(updatedTrafficControl.getBundle());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortList, vendorPortList, startOfDay, startOfDay);
                        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                            value += performanceClientDay.getEventC() + performanceClientDay.getEventD() + performanceClientDay.getEventE() + performanceClientDay.getEventF() + performanceClientDay.getEventK()
                                + performanceClientDay.getEventO();
                        }
                    } else {
                        List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(clientPortList, vendorPortList, bundleList, startOfDay, startOfDay);
                        for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                            value += performanceClientBundleDay.getEventC() + performanceClientBundleDay.getEventD() + performanceClientBundleDay.getEventE() + performanceClientBundleDay.getEventF() + performanceClientBundleDay.getEventK()
                                + performanceClientBundleDay.getEventO();
                        }
                    }
                }

                Long expire = 86400L + 60;

                RedisTemplate<String, String> redisTemplate = redisTemplate();
                redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(expire));
            }
            if (updatedTrafficControl.getIndicator() == TrafficControl.TC_INDICATOR_REQUEST
                && updatedTrafficControl.getPeriod() == TrafficControl.TC_PERIOD_HOUR) {
                Integer hour = time.toInstant().atZone(ZoneId.of("GMT+00:00")).getHour();
                Timestamp startOfHour = new Timestamp(time.toInstant().atZone(ZoneId.of("GMT+00:00")).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());

                String key = String.format("QRH%02d:%d:%d:%s",
                    hour,
                    updatedTrafficControl.getClientPort(),
                    updatedTrafficControl.getVendorPort(),
                    StringUtils.replace(updatedTrafficControl.getBundle(), ":", "_"));
                Long value = 0L;

                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() == -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfHour, startOfHour);
                        for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                            value += performanceClientHour.getEventC() + performanceClientHour.getEventD() + performanceClientHour.getEventE() + performanceClientHour.getEventF() + performanceClientHour.getEventK()
                                + performanceClientHour.getEventO();
                        }
                    } else {
                        List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfHour, startOfHour);
                        for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                            value += performanceClientBundleHour.getEventC() + performanceClientBundleHour.getEventD() + performanceClientBundleHour.getEventE() + performanceClientBundleHour.getEventF() + performanceClientBundleHour.getEventK()
                                + performanceClientBundleHour.getEventO();
                        }
                    }
                }
                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() != -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    List<Integer> vendorPortList = List.of(updatedTrafficControl.getVendorPort());
                    List<String> bundleList = List.of(updatedTrafficControl.getBundle());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortList, vendorPortList, startOfHour, startOfHour);
                        for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                            value += performanceClientHour.getEventC() + performanceClientHour.getEventD() + performanceClientHour.getEventE() + performanceClientHour.getEventF() + performanceClientHour.getEventK()
                                + performanceClientHour.getEventO();
                        }
                    } else {
                        List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(clientPortList, vendorPortList, bundleList, startOfHour, startOfHour);
                        for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                            value += performanceClientBundleHour.getEventC() + performanceClientBundleHour.getEventD() + performanceClientBundleHour.getEventE() + performanceClientBundleHour.getEventF() + performanceClientBundleHour.getEventK()
                                + performanceClientBundleHour.getEventO();
                        }
                    }
                }

                Long expire = 3600L + 60;

                RedisTemplate<String, String> redisTemplate = redisTemplate();
                redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(expire));
            }

            if (updatedTrafficControl.getIndicator() == TrafficControl.TC_INDICATOR_COST
                && updatedTrafficControl.getPeriod() == TrafficControl.TC_PERIOD_DAY) {
                Integer day = time.toInstant().atZone(ZoneId.of("GMT+08:00")).getDayOfMonth();
                Timestamp startOfDay = new Timestamp(time.toInstant().atZone(ZoneId.of("GMT+08:00")).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());

                String key = String.format("QCD%02d:%d:%d:%s",
                    day,
                    updatedTrafficControl.getClientPort(),
                    updatedTrafficControl.getVendorPort(),
                    StringUtils.replace(updatedTrafficControl.getBundle(), ":", "_"));
                Long value = 0L;

                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() == -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfDay, startOfDay);
                        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                            value += performanceClientDay.getIncome();
                        }
                    } else {
                        List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfDay, startOfDay);
                        for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                            value += performanceClientBundleDay.getIncome();
                        }
                    }
                }
                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() != -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    List<Integer> vendorPortList = List.of(updatedTrafficControl.getVendorPort());
                    List<String> bundleList = List.of(updatedTrafficControl.getBundle());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortList, vendorPortList, startOfDay, startOfDay);
                        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
                            value += performanceClientDay.getIncome();
                        }
                    } else {
                        List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(clientPortList, vendorPortList, bundleList, startOfDay, startOfDay);
                        for (PerformanceClientBundleDay performanceClientBundleDay : performanceClientBundleDayList) {
                            value += performanceClientBundleDay.getIncome();
                        }
                    }
                }

                Long expire = 86400L + 60;

                RedisTemplate<String, String> redisTemplate = redisTemplate();
                redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(expire));
            }
            if (updatedTrafficControl.getIndicator() == TrafficControl.TC_INDICATOR_COST
                && updatedTrafficControl.getPeriod() == TrafficControl.TC_PERIOD_HOUR) {
                Integer hour = time.toInstant().atZone(ZoneId.of("GMT+00:00")).getHour();
                Timestamp startOfHour = new Timestamp(time.toInstant().atZone(ZoneId.of("GMT+00:00")).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli());

                String key = String.format("QCH%02d:%d:%d:%s",
                    hour,
                    updatedTrafficControl.getClientPort(),
                    updatedTrafficControl.getVendorPort(),
                    StringUtils.replace(updatedTrafficControl.getBundle(), ":", "_"));
                Long value = 0L;

                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() == -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfHour, startOfHour);
                        for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                            value += performanceClientHour.getIncome();
                        }
                    } else {
                        List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortList, startOfHour, startOfHour);
                        for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                            value += performanceClientBundleHour.getIncome();
                        }
                    }
                }
                if (updatedTrafficControl.getClientPort() != -1 && updatedTrafficControl.getVendorPort() != -1) {
                    List<Integer> clientPortList = List.of(updatedTrafficControl.getClientPort());
                    List<Integer> vendorPortList = List.of(updatedTrafficControl.getVendorPort());
                    List<String> bundleList = List.of(updatedTrafficControl.getBundle());
                    String bundle = updatedTrafficControl.getBundle();

                    if (StringUtils.isEmpty(bundle)) {
                        List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortList, vendorPortList, startOfHour, startOfHour);
                        for (PerformanceClientHour performanceClientHour : performanceClientHourList) {
                            value += performanceClientHour.getIncome();
                        }
                    } else {
                        List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findDetailByClientPortInAndVendorPortInAndBundleInAndTimeBetween(clientPortList, vendorPortList, bundleList, startOfHour, startOfHour);
                        for (PerformanceClientBundleHour performanceClientBundleHour : performanceClientBundleHourList) {
                            value += performanceClientBundleHour.getIncome();
                        }
                    }
                }

                Long expire = 3600L + 60;

                RedisTemplate<String, String> redisTemplate = redisTemplate();
                redisTemplate.opsForValue().set(key, String.valueOf(value), Duration.ofSeconds(expire));
            }
        }
    }

    @Transactional
    public void removeTrafficControl(TrafficControl targetTrafficControl) {
        if (targetTrafficControl != null) {
            trafficControlRepository.delete(targetTrafficControl);
        }
    }

}
