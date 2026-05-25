package backend.academy.linktracker.scrapper.client;

import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.KAFKA_CONTAINER;
import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.SCHEMA_REGISTRY;
import static backend.academy.linktracker.scrapper.config.TestBeans.POSTGRES;
import static backend.academy.linktracker.scrapper.config.TestBeans.VALKEY;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.config.KafkaConfiguration;
import backend.academy.linktracker.scrapper.config.TestBeans;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@WireMockTest(httpPort = 54321)
@Import({TestBeans.class, KafkaConfiguration.class})
public class ExternalClientsRetryExponentialBackoffTest {

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
        VALKEY.start();
    }

    @Autowired
    private GitHubClient gitHubClient;

    @Autowired
    private StackOverflowClient stackOverflowClient;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("resilience4j.retry.instances.external-exponent.wait-duration", () -> "500ms");
        registry.add("resilience4j.retry.instances.external-exponent.max-attempts", () -> 3);
        registry.add("app.communication.client.read-timeout", () -> Duration.ofSeconds(1));

        registry.add("app.github.base-url", () -> "http://localhost:54321");
        registry.add("app.stackoverflow.base-url", () -> "http://localhost:54321");
        registry.add("app.stackoverflow.key", () -> "test-key");

        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
                "app.kafka.schema-registry",
                () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
        registry.add("app.redis.time-to-live", () -> "2s");
    }

    @Test
    public void TC4_StackOverflow_VerifyRetryIntervalIsExponential() {
        // given
        String path = "/questions/123/answers";
        String scenarioName = "so_exponential_backoff";

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500).withFixedDelay(0))
                .willSetStateTo("first failure"));

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse().withStatus(500).withFixedDelay(0))
                .willSetStateTo("second failure"));

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // when
        long startTime = System.currentTimeMillis();
        stackOverflowClient.fetchAnswers("123", 1600000000L);
        long endTime = System.currentTimeMillis();

        // then
        List<LoggedRequest> requests = WireMock.findAll(getRequestedFor(urlPathEqualTo(path)));
        assertThat(requests).hasSize(3);

        long[] timestamps =
                requests.stream().mapToLong(r -> r.getLoggedDate().getTime()).toArray();

        long interval1to2 = timestamps[1] - timestamps[0];
        long interval2to3 = timestamps[2] - timestamps[1];

        assertThat(interval1to2).isBetween(400L, 1000L);
        assertThat(interval2to3).isBetween(1000L, 2000L);

        long totalDuration = endTime - startTime;
        assertThat(totalDuration).isGreaterThan(1500L);
    }
}
