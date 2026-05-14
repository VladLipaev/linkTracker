package backend.academy.linktracker.scrapper.config;

import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.KAFKA_CONTAINER;
import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.SCHEMA_REGISTRY;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestBeans {

    public static final Network SHARED_NETWORK = Network.newNetwork();

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("postgres")
            .withDatabaseName("scrapper")
            .withUsername("user")
            .withPassword("pass");

    @Bean
    public Network network() {
        return SHARED_NETWORK;
    }

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }

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

    public static final GenericContainer<?> VALKEY =
            new GenericContainer<>("valkey/valkey:latest").withExposedPorts(6379);

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
        VALKEY.start();
    }
}
