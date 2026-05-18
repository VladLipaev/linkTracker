package backend.academy.linktracker.scrapper.client.bot;

import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.KAFKA_CONTAINER;
import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.SCHEMA_REGISTRY;
import static backend.academy.linktracker.scrapper.config.TestBeans.POSTGRES;
import static backend.academy.linktracker.scrapper.config.TestBeans.VALKEY;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.withinPercentage;

import backend.academy.linktracker.scrapper.config.KafkaConfiguration;
import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
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
public class BotClientRetryTest {

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
        VALKEY.start();
    }

    @Autowired
    private TelegramBotClient botClient;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("resilience4j.retry.instances.bot.wait-duration", () -> "500ms");
        registry.add("app.communication.client.read-timeout", () -> Duration.ofSeconds(1));
        registry.add("resilience4j.retry.instances.bot.max-attempts", () -> 3);
        registry.add("app.bot.uri", () -> "http://localhost:54321");
        registry.add("resilience4j.circuitbreaker.instances.bot.enabled", () -> false);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
                "app.kafka.schema-registry",
                () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
        registry.add("app.redis.time-to-live", () -> "2s");
    }

    @Test
    public void TC2_1_2TimesReturn500_LastTimeReturns200() {
        // given
        LinkUpdate update = new LinkUpdate(1L, "https://example.com", "desc", List.of(100L));
        String scenarioName = "retry500";

        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("first failure"));

        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second failure"));

        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse().withStatus(200)));

        // when
        botClient.sendUpdate(update);

        // then
        WireMock.verify(3, postRequestedFor(urlEqualTo("/updates")));
    }

    @Test
    public void TC2_2_400Error_NoRetry() {
        // given
        LinkUpdate update = new LinkUpdate(1L, "https://example.com", "desc", List.of(100L));
        String scenarioName = "retry400";

        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(400))
                .willSetStateTo("first failure"));

        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse().withStatus(401))
                .willSetStateTo("second failure"));

        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse().withStatus(404)));

        // when (бизнес ошибки не перехватываются фолбеком, так как это не Throwable, а фильтруемые в ignoreExceptions)
        Throwable thrown = catchThrowable(() -> botClient.sendUpdate(update));

        // then
        assertThat(thrown).isInstanceOf(BotClientException.class).hasMessageContaining("Ошибка клиента: ");
        WireMock.verify(1, postRequestedFor(urlEqualTo("/updates")));
    }

    @Test
    public void TC2_3_VerifyRetryIntervalIsConstant() {
        // given
        LinkUpdate update = new LinkUpdate(1L, "https://example.com", "desc", List.of(100L));
        String scenarioName = "constant_interval";

        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("first failure"));
        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second failure"));
        stubFor(post(urlEqualTo("/updates"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse().withStatus(200)));

        // when
        botClient.sendUpdate(update);

        // then
        List<LoggedRequest> requests = WireMock.findAll(postRequestedFor(urlEqualTo("/updates")));
        assertThat(requests).hasSize(3);

        long time1 = requests.get(0).getLoggedDate().getTime();
        long time2 = requests.get(1).getLoggedDate().getTime();
        long time3 = requests.get(2).getLoggedDate().getTime();

        long interval1to2 = time2 - time1;
        long interval2to3 = time3 - time2;
        long expectedInterval = 500L;

        assertThat(interval1to2).isGreaterThanOrEqualTo(expectedInterval).isLessThan(expectedInterval + 200L);
        assertThat(interval2to3).isGreaterThanOrEqualTo(expectedInterval).isLessThan(expectedInterval + 200L);
        assertThat((double) interval2to3).isCloseTo((double) interval1to2, withinPercentage(20.0));
    }
}
