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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.config.KafkaConfiguration;
import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.service.kafka.KafkaNotificationUpdateSender;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@WireMockTest(httpPort = 54321)
@Import({TestBeans.class, KafkaConfiguration.class})
public class BotClientCircuitBreakerTest {

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
        VALKEY.start();
    }

    @Autowired
    private TelegramBotClient botClient;

    @MockitoBean
    private KafkaNotificationUpdateSender updateSender;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker cb;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("app.bot.uri", () -> "http://localhost:54321");
        registry.add("app.communication.client.read-timeout", () -> "1s");
        registry.add("resilience4j.retry.instances.bot.max-attempts", () -> 1);
        registry.add("resilience4j.circuitbreaker.instances.bot.minimum-number-of-calls", () -> 5);
        registry.add("resilience4j.circuitbreaker.instances.bot.sliding-window-size", () -> 5);
        registry.add("resilience4j.circuitbreaker.instances.bot.failure-rate-threshold", () -> 50);
        registry.add("resilience4j.circuitbreaker.instances.bot.wait-duration-in-open-state", () -> "1s");
        registry.add("resilience4j.circuitbreaker.instances.bot.permitted-number-of-calls-in-half-open-state", () -> 3);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
                "app.kafka.schema-registry",
                () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
        registry.add("app.redis.time-to-live", () -> "2s");
    }

    @BeforeEach
    void resetCircuitBreaker() {
        cb = circuitBreakerRegistry.circuitBreaker("bot");
        cb.reset();
    }

    @Test
    public void TC4_1_ShouldTransitionToOpenAndShortCircuit() {
        // given
        stubFor(post(urlEqualTo("/updates")).willReturn(aResponse().withStatus(500)));
        LinkUpdate update = new LinkUpdate(1L, "https://example.com", "desc", List.of(100L));

        // when
        for (int i = 0; i < 5; i++) {
            botClient.sendUpdate(update);
            if (i < 4) {
                assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            }
        }

        // then
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        WireMock.resetAllRequests();

        botClient.sendUpdate(update);

        WireMock.verify(0, postRequestedFor(urlEqualTo("/updates")));
        verify(updateSender, times(6)).sendUpdate(update);
    }

    @Test
    public void TC4_2_HalfOpenToClosedStateAfterSuccessfulPermittedCalls() throws InterruptedException {
        // given
        stubFor(post(urlEqualTo("/updates")).willReturn(aResponse().withStatus(500)));
        LinkUpdate update = new LinkUpdate(1L, "https://example.com", "desc", List.of(100L));

        for (int i = 0; i < 5; i++) {
            botClient.sendUpdate(update);
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        Thread.sleep(1100);

        stubFor(post(urlEqualTo("/updates")).willReturn(aResponse().withStatus(200)));

        for (int i = 0; i < 3; i++) {
            botClient.sendUpdate(update);
            if (i < 2) {
                assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            }
        }
        // then
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void TC4_3_HalfOpenToOpenStateAfterFailedPermittedCalls() throws InterruptedException {
        // given
        stubFor(post(urlEqualTo("/updates")).willReturn(aResponse().withStatus(500)));
        LinkUpdate update = new LinkUpdate(1L, "https://example.com", "desc", List.of(100L));

        for (int i = 0; i < 5; i++) {
            botClient.sendUpdate(update);
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        Thread.sleep(1100);

        for (int i = 0; i < 3; i++) {
            botClient.sendUpdate(update);
            if (i == 0) {
                assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            }
        }

        // then
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        WireMock.resetAllRequests();
        botClient.sendUpdate(update);
        WireMock.verify(0, postRequestedFor(urlEqualTo("/updates")));
    }

    @Test
    public void TC5_1_CircuitBreakerIsOpen_HttpNotAllowed_shouldSendToFallbackKafka() {
        // given
        stubFor(post(urlEqualTo("/updates")).willReturn(aResponse().withStatus(500)));
        LinkUpdate update = new LinkUpdate(1L, "https://example.com", "desc", List.of(100L));

        for (int i = 0; i < 5; i++) {
            botClient.sendUpdate(update);
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        clearInvocations(updateSender);

        // when
        botClient.sendUpdate(update);

        // then
        verify(updateSender, times(1)).sendUpdate(update);
    }
}
