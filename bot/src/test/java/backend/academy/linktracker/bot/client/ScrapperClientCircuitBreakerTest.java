package backend.academy.linktracker.bot.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.AbstractIntegrationTest;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.config.TestBeans;
import backend.academy.linktracker.bot.dto.avro.AddLinkMessageAvro;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@WireMockTest(httpPort = 54321)
@Import(TestBeans.class)
public class ScrapperClientCircuitBreakerTest extends AbstractIntegrationTest {

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ScrapperClient scrapperClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker cb;

    @Value("${app.communication.client.kafka-fallback.topic.link-add}")
    private String addTopic;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("app.scrapper.uri", () -> "http://localhost:54321");
        registry.add("app.communication.client.read-timeout", () -> "1s");
        registry.add("resilience4j.retry.instances.scrapper.max-attempts", () -> 1);
        registry.add("resilience4j.circuitbreaker.instances.scrapper.minimum-number-of-calls", () -> 5);
        registry.add("resilience4j.circuitbreaker.instances.scrapper.sliding-window-size", () -> 5);
        registry.add("resilience4j.circuitbreaker.instances.scrapper.failure-rate-threshold", () -> 50);
        registry.add("resilience4j.circuitbreaker.instances.scrapper.wait-duration-in-open-state", () -> "1s");
        registry.add(
                "resilience4j.circuitbreaker.instances.scrapper.permitted-number-of-calls-in-half-open-state", () -> 3);
    }

    @BeforeEach
    void resetCircuitBreaker() {
        cb = circuitBreakerRegistry.circuitBreaker("scrapper");
        cb.reset();
    }

    @Test
    public void TC4_1_ShouldTransitionToOpenAndShortCircuit() {
        // given
        stubFor(post(urlEqualTo("/links")).willReturn(aResponse().withStatus(500)));

        // when
        for (int i = 0; i < 5; i++) {
            Throwable thrown = catchThrowable(() -> scrapperClient.addLink(100L, "https://example.com", List.of()));
            assertThat(thrown).isInstanceOf(ScrapperClientException.class);

            if (i < 4) {
                assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            }
        }

        // then
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        WireMock.resetAllRequests();

        Throwable shortCircuitThrown =
                catchThrowable(() -> scrapperClient.addLink(100L, "https://example.com", List.of()));
        assertThat(shortCircuitThrown).isInstanceOf(ScrapperClientException.class);

        // запрос не долетел до WireMock
        WireMock.verify(0, postRequestedFor(urlEqualTo("/links")));
    }

    @Test
    public void TC4_2_HalfOpenToClosedStateAfterSuccessfulPermittedCalls() throws InterruptedException {
        // given
        stubFor(post(urlEqualTo("/links")).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 5; i++) {
            catchThrowable(() -> scrapperClient.addLink(100L, "https://example.com", List.of()));
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        Thread.sleep(1100);
        stubFor(post(urlEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"url\": \"https://example.com\", \"tags\": []}")));

        for (int i = 0; i < 3; i++) {
            scrapperClient.addLink(100L, "https://example.com", List.of());
            if (i < 2) {
                assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            }
        }
        // then
        // Circuit Breaker убедился, что сервер здоров, и перешел в штатный режим (CLOSED)
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void TC4_3_HalfOpenToOpenStateAfterFailedPermittedCalls() throws InterruptedException {
        // given
        stubFor(post(urlEqualTo("/links")).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 5; i++) {
            catchThrowable(() -> scrapperClient.addLink(100L, "https://example.com", List.of()));
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        Thread.sleep(1100);
        for (int i = 0; i < 3; i++) {
            catchThrowable(() -> scrapperClient.addLink(100L, "https://example.com", List.of()));

            if (i == 0) {
                assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            }
        }

        // then
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        WireMock.resetAllRequests();
        catchThrowable(() -> scrapperClient.addLink(100L, "https://example.com", List.of()));
        WireMock.verify(0, postRequestedFor(urlEqualTo("/links")));
    }

    @Test
    public void TC5_1_CircuitBreakerIsOpen_HttpNotAllowed_shouldSendToFallbackKafka() {
        // given
        stubFor(post(urlEqualTo("/links")).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 5; i++) {
            catchThrowable(() -> scrapperClient.addLink(100L, "https://example.com", List.of()));
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        clearInvocations(kafkaTemplate);
        // when
        catchThrowable(() -> scrapperClient.addLink(100L, "https://example.com", List.of()));

        // then
        verify(kafkaTemplate).send(addTopic, new AddLinkMessageAvro(100L, "https://example.com", List.of()));
    }
}
