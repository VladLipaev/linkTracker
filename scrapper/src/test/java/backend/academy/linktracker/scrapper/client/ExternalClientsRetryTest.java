package backend.academy.linktracker.scrapper.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.withinPercentage;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@WireMockTest(httpPort = 54321)
@ActiveProfiles("test-external-constant-backoff")
public class ExternalClientsRetryTest extends AbstractIntegrationTest {

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

        registry.add("app.redis.time-to-live", () -> "2s");
    }

    @Test
    public void TC1_GitHub_2TimesReturn500_LastTimeReturns200() {
        // given
        String owner = "owner";
        String repo = "repo";
        String scenarioName = "github_retry500";
        String path = "/repos/owner/repo/issues";

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("first failure"));

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second failure"));

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        // when
        gitHubClient.fetchRepo(owner, repo, OffsetDateTime.now(), 1, 50);

        // then
        WireMock.verify(3, getRequestedFor(urlPathEqualTo(path)));
    }

    @Test
    public void TC2_GitHub_400Error_NoRetry() {
        // given
        String path = "/repos/owner/repo/issues";

        stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withStatus(404)));

        // when
        Throwable thrown = catchThrowable(() -> gitHubClient.fetchRepo("owner", "repo", OffsetDateTime.now(), 1, 50));

        // then
        assertThat(thrown).isInstanceOf(GitHubClientException.class);
        WireMock.verify(1, getRequestedFor(urlPathEqualTo(path)));
    }

    @Test
    public void TC3_StackOverflow_2TimesReturn500_LastTimeReturns200() {
        // given
        String questionId = "123";
        String scenarioName = "so_retry500";
        String path = "/questions/123";

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("first failure"));

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second failure"));

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // when
        stackOverflowClient.fetchQuestion(questionId);

        // then
        WireMock.verify(3, getRequestedFor(urlPathEqualTo(path)));
    }

    @Test
    public void TC4_StackOverflow_VerifyRetryIntervalIsConstant() {
        // given
        String path = "/questions/123/answers";
        String scenarioName = "so_constant_interval";

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("first failure"));

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs("first failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second failure"));

        stubFor(get(urlPathEqualTo(path))
                .inScenario(scenarioName)
                .whenScenarioStateIs("second failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // when
        stackOverflowClient.fetchAnswers("123", 1600000000L);

        // then
        List<LoggedRequest> requests = WireMock.findAll(getRequestedFor(urlPathEqualTo(path)));
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
