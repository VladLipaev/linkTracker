package backend.academy.linktracker.scrapper;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    public static final Network SHARED_NETWORK = Network.newNetwork();

    public static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:17")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("postgres")
            .withDatabaseName("scrapper")
            .withUsername("user")
            .withPassword("pass");

    public static final ConfluentKafkaContainer KAFKA_CONTAINER =
        new ConfluentKafkaContainer("confluentinc/cp-kafka:7.5.0")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("broker");

    public static final GenericContainer<?> SCHEMA_REGISTRY =
        new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-schema-registry:7.5.0")
        )
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("schema-registry")
            .withExposedPorts(9081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:9081")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://broker:9093")
            .dependsOn(KAFKA_CONTAINER)
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    public static final GenericContainer<?> VALKEY =
        new GenericContainer<>("valkey/valkey:latest")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("valkey")
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
        VALKEY.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.username", POSTGRES::getUsername);

        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
            "app.kafka.schema-registry",
            () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort()
        );
    }

    @Configuration
    static class TestConfig {
        @Bean
        public SpringLiquibase liquibase(DataSource dataSource) {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog("classpath:migrations/db.changelog-master.xml");
            return liquibase;
        }

        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(VALKEY.getHost(), VALKEY.getFirstMappedPort());
        }
    }
}
