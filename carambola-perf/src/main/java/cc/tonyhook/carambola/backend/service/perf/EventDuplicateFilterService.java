package cc.tonyhook.carambola.backend.service.perf;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.SetCondition;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.entity.perf.Event;
import io.lettuce.core.api.StatefulConnection;

@Service
public class EventDuplicateFilterService {

    private static final int DUPLICATE_TTL_SECONDS = 3600;

    private static final List<String> ID_KEYS = List.of(
        "imei",
        "imei_md5",
        "android_id",
        "android_id_md5",
        "oaid",
        "oaid_md5",
        "idfa",
        "idfa_md5",
        "idfv",
        "idfv_md5",
        "caid",
        "caid1",
        "caid1_md5",
        "caid2",
        "caid2_md5",
        "aaid",
        "mac",
        "mac_md5"
    );

    @Value("${app.event-deduplicate-repository:localhost}")
    private String eventDeduplicateRepository;

    @Value("${app.event-deduplicate-repository-port:6379}")
    private Integer eventDeduplicateRepositoryPort;

    private RedisConnectionFactory redisConnectionFactory = null;

    public boolean markIfDuplicate(Event event, Map<String, String> cleanQueries) {
        String key = buildKey(event, cleanQueries);
        if (key == null) {
            return false;
        }

        try {
            Boolean inserted = redisTemplate().execute((RedisCallback<Boolean>) connection ->
                connection.stringCommands().set(
                    key.getBytes(StandardCharsets.UTF_8),
                    "1".getBytes(StandardCharsets.UTF_8),
                    SetCondition.ifAbsent(),
                    Expiration.from(DUPLICATE_TTL_SECONDS, TimeUnit.SECONDS)
                )
            );

            return Boolean.FALSE.equals(inserted);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildKey(Event event, Map<String, String> queries) {
        if (event == null || event.getClientChannel() == null || event.getClientChannel().getId() == null) {
            return null;
        }
        if (StringUtils.isBlank(event.getEvent()) || queries == null) {
            return null;
        }

        List<String> parts = new ArrayList<String>();
        for (String idKey : ID_KEYS) {
            String value = normalize(queries.get(idKey));
            if (value != null) {
                parts.add(idKey + "=" + value);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }

        String signature = String.join("&", parts);
        return "perf:event:duplicate:"
            + event.getClientChannel().getId()
            + ":"
            + event.getEvent()
            + ":"
            + sha256(signature);
    }

    private String normalize(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return String.valueOf(value.hashCode());
        }
    }

    private RedisConnectionFactory redisConnectionFactory() {
        if (redisConnectionFactory != null) {
            return redisConnectionFactory;
        }

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(
            eventDeduplicateRepository,
            eventDeduplicateRepositoryPort
        );

        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofSeconds(2))
            .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
        factory.afterPropertiesSet();
        this.redisConnectionFactory = factory;

        return redisConnectionFactory;
    }

    private RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);
        template.afterPropertiesSet();

        return template;
    }

}
