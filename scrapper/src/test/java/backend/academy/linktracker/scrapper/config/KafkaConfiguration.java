package backend.academy.linktracker.scrapper.config;

import static backend.academy.linktracker.scrapper.config.TestBeans.SHARED_NETWORK;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaConfiguration {

    public static final ConfluentKafkaContainer KAFKA_CONTAINER = new ConfluentKafkaContainer(
                    "confluentinc/cp-kafka:7.5.0")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("broker");

    public static final GenericContainer<?> SCHEMA_REGISTRY = new GenericContainer<>(
                    DockerImageName.parse("confluentinc/cp-schema-registry:7.5.0"))
            .withNetwork(SHARED_NETWORK)
            .withExposedPorts(9081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:9081")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://broker:9093")
            .dependsOn(KAFKA_CONTAINER)
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
}
