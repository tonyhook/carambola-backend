package cc.tonyhook.carambola.backend.service.perf;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import io.lettuce.core.api.StatefulConnection;
import jakarta.annotation.PreDestroy;

@Service
public class PerfDebugPrintService {

    public static final String REDIS_KEY = "perf:event:debug-print";

    private static final Set<String> ENABLED_VALUES = Set.of("1", "true", "on", "yes");

    private final LettuceConnectionFactory connectionFactory;
    private final RedisTemplate<String, String> redisTemplate;

    public PerfDebugPrintService(
            @Value("${app.event-deduplicate-repository:localhost}") String repository,
            @Value("${app.event-deduplicate-repository-port:6379}") Integer repositoryPort
    ) {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(repository, repositoryPort);

        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(4);
        poolConfig.setMaxIdle(2);
        poolConfig.setMinIdle(0);

        LettucePoolingClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofSeconds(1))
            .build();

        this.connectionFactory = new LettuceConnectionFactory(redisConfiguration, clientConfiguration);
        this.connectionFactory.afterPropertiesSet();

        StringRedisSerializer serializer = new StringRedisSerializer();
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(this.connectionFactory);
        this.redisTemplate.setKeySerializer(serializer);
        this.redisTemplate.setValueSerializer(serializer);
        this.redisTemplate.afterPropertiesSet();
    }

    public void println(String message) {
        if (isEnabled()) {
            System.out.println(message);
        }
    }

    private boolean isEnabled() {
        try {
            String value = redisTemplate.opsForValue().get(REDIS_KEY);
            return value != null && ENABLED_VALUES.contains(value.trim().toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void destroy() {
        connectionFactory.destroy();
    }
}
