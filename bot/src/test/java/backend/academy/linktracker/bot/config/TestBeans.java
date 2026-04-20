package backend.academy.linktracker.bot.config;

import backend.academy.linktracker.bot.client.scrapper.RestClientScrapperRestClient;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class TestBeans {

    private static final String DLQ_TOPIC = "link-updates-topic-dlt";

    @Bean
    @Primary
    public RestClientScrapperRestClient restClient(
            @Value("${app.scrapper.uri:http://localhost:54321}") String scrapperBaseUri) {
        return new RestClientScrapperRestClient(
                RestClient.builder().baseUrl(scrapperBaseUri).build());
    }
    //
    //    public static final Network SHARED_NETWORK = Network.newNetwork();
    //
    //    @Container
    //    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
    //        .withNetwork(SHARED_NETWORK)
    //        .withNetworkAliases("postgres")
    //        .withDatabaseName("scrapper")
    //        .withUsername("user")
    //        .withPassword("pass");
    //
    //    @Bean
    //    public Network network() {
    //        return SHARED_NETWORK;
    //    }
    //
    //    @Container
    //    public static final ConfluentKafkaContainer KAFKA_CONTAINER =
    //        new ConfluentKafkaContainer("confluentinc/cp-kafka:7.5.0")
    //            .withNetwork(SHARED_NETWORK).withNetworkAliases("broker");
    //
    //    @Container
    //    public static final GenericContainer<?> SCHEMA_REGISTRY =
    //        new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.5.0"))
    //            .withNetwork(SHARED_NETWORK)
    //            .withExposedPorts(8081)
    //            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
    //            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
    //            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
    //                "PLAINTEXT://broker:9093")
    //            .dependsOn(KAFKA_CONTAINER)
    //            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
    //
    //
    //    @Bean
    //    @ServiceConnection
    //    public PostgreSQLContainer postgresContainer() {
    //        return POSTGRES;
    //    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:migrations/db.changelog-master.xml");
        return liquibase;
    }

    @Bean
    public DlqListenerWatcher dlqListenerWatcher() {
        return new DlqListenerWatcher();
    }

    // 2. Убираем аннотацию @Component отсюда
    public static class DlqListenerWatcher {
        @KafkaListener(topics = DLQ_TOPIC, groupId = "test-dlq-group")
        public void listenDlq(ConsumerRecord<String, Object> record) {
            // Тело может быть пустым, нам важен сам факт вызова
        }
    }
}
