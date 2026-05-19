package backend.academy.linktracker.bot.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.withinPercentage;

import backend.academy.linktracker.bot.AbstractIntegrationTest;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.config.TestBeans;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@WireMockTest(httpPort = 54321)
@Import(TestBeans.class)
public class ScrapperClientRetryTest extends AbstractIntegrationTest {

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ScrapperClient scrapperClient;

    @MockitoBean
    private TelegramUpdateService telegramUpdateService;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("resilience4j.retry.instances.scrapper.wait-duration", () -> "500ms");
        registry.add("app.communication.client.read-timeout", () -> Duration.ofSeconds(1));
        registry.add("resilience4j.retry.instances.scrapper.max-attempts", () -> 3);
        registry.add("app.scrapper.uri", () -> "http://localhost:54321");
        registry.add("resilience4j.circuitbreaker.instances.scrapper.enabled", () -> false);
    }

    @Test
    public void TC2_1_2TimesReturn500_LastTimeReturns200() {
        // given
        long chatId = 100L;
        String url = "https://example.com";
        List<String> tags = List.of();
        String scenarioName = "retry500";
        stubFor(post(urlEqualTo("/tg-chat/100"))
                .willReturn(aResponse().withStatus(200)));

        stubFor(post(urlEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
        scrapperClient.registerChat(chatId);
        scrapperClient.addLink(chatId, url, tags);

        stubFor(get(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                        aResponse()
                                .withStatus(500)
                                .withHeader("Content-Type", "application/json") // важно!
                                .withBody("") // пустое тело, но валидное
                        )
                .willSetStateTo("first failure"));

        stubFor(get(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody(""))
                .willSetStateTo("second failure"));

        stubFor(get(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"links\": [{\"id\": 1, \"url\": \"" + url + "\", \"tags\": []}], \"size\": 1}")));

        // when
        ListLinksResponse listLinksResponse = scrapperClient.getLinks(chatId, null);

        // then
        assertThat(listLinksResponse).isNotNull();
        assertThat(listLinksResponse.links().getFirst().url()).isEqualTo(url);
        WireMock.verify(3, getRequestedFor(urlEqualTo("/links")));
    }

    @Test
    public void TC2_2_400Error_NoRetry() {
        // given
        long chatId = 100L;
        String url = "https://example.com";
        List<String> tags = List.of();
        String scenarioName = "retry400";

        stubFor(post(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                        aResponse()
                                .withStatus(400)
                                .withHeader("Content-Type", "application/json") // важно!
                                .withBody("") // пустое тело, но валидное
                        )
                .willSetStateTo("first failure"));

        stubFor(post(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody(""))
                .willSetStateTo("second failure"));

        stubFor(post(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // when
        Throwable thrown1 = catchThrowable(() -> scrapperClient.addLink(chatId, url, tags));
        Throwable thrown2 = catchThrowable(() -> scrapperClient.addLink(chatId, url, tags));
        Throwable thrown3 = catchThrowable(() -> scrapperClient.addLink(chatId, url, tags));

        // then
        assertThat(thrown1).isInstanceOf(ScrapperClientException.class).hasMessageContaining("Ошибка клиента: ");
        assertThat(thrown2).isInstanceOf(ScrapperClientException.class).hasMessageContaining("Ошибка клиента: ");
        assertThat(thrown3).isInstanceOf(ScrapperClientException.class).hasMessageContaining("Ошибка клиента: ");
        WireMock.verify(3, postRequestedFor(urlEqualTo("/links")));
    }

    @Test
    public void TC2_3_VerifyRetryIntervalIsConstant() {
        // given
        long chatId = 100L;
        String url = "https://example.com";
        List<String> tags = List.of();
        String scenarioName = "constant_interval";

        stubFor(get(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("first failure"));

        stubFor(get(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second failure"));

        stubFor(get(urlEqualTo("/links"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // when
        scrapperClient.getLinks(chatId, null);

        // then
        List<LoggedRequest> requests = WireMock.findAll(getRequestedFor(urlEqualTo("/links")));

        assertThat(requests).hasSize(3);

        long time1 = requests.get(0).getLoggedDate().getTime();
        long time2 = requests.get(1).getLoggedDate().getTime();
        long time3 = requests.get(2).getLoggedDate().getTime();

        long interval1to2 = time2 - time1;
        long interval2to3 = time3 - time2;

        long expectedInterval = 500L;

        assertThat(interval1to2).isGreaterThanOrEqualTo(expectedInterval).isLessThan(expectedInterval + 200L);

        assertThat(interval2to3).isGreaterThanOrEqualTo(expectedInterval).isLessThan(expectedInterval + 200L);

        // экспоненциальный backoff не используется
        // для этого интервалы должны быть примерно одинаковыми
        assertThat((double) interval2to3).isCloseTo((double) interval1to2, withinPercentage(20.0));
    }
}
