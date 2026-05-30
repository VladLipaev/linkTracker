package backend.academy.linktracker.ai;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    // Создаем сеть для связи Kafka и Schema Registry
    public static final Network SHARED_NETWORK = Network.newNetwork();

    // Объявляем контейнеры статически
    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("postgres")
            .withDatabaseName("scrapper")
            .withUsername("user")
            .withPassword("pass");

    public static final ConfluentKafkaContainer KAFKA_CONTAINER = new ConfluentKafkaContainer(
                    "confluentinc/cp-kafka:7.5.0")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("broker");

    public static final GenericContainer<?> SCHEMA_REGISTRY = new GenericContainer<>(
                    DockerImageName.parse("confluentinc/cp-schema-registry:7.5.0"))
            .withNetwork(SHARED_NETWORK)
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://broker:9093")
            .dependsOn(KAFKA_CONTAINER)
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.username", POSTGRES::getUsername);

        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
                "app.kafka.schema-registry",
                () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
    }
}
