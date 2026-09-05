package backend.academy.linktracker.bot;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Tag("integration")
@Tag("redis")
public abstract class AbstractRedisTest {

    public static final GenericContainer<?> VALKEY =
        new GenericContainer<>("valkey/valkey:latest")
            .withNetworkAliases("valkey")
            .withExposedPorts(6379);

    static {
        VALKEY.start();
    }
    // Если @ServiceConnection не работает, можно задать свойства вручную:
    // @DynamicPropertySource
    // static void redisProperties(DynamicPropertyRegistry registry) {
    //     registry.add("spring.data.redis.host", redisContainer::getHost);
    //     registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    // }
}
