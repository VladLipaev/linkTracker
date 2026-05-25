package backend.academy.linktracker.scrapper.client.bot;

import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.KAFKA_CONTAINER;
import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.SCHEMA_REGISTRY;
import static backend.academy.linktracker.scrapper.config.TestBeans.POSTGRES;
import static backend.academy.linktracker.scrapper.config.TestBeans.VALKEY;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.config.KafkaConfiguration;
import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.service.kafka.KafkaNotificationUpdateSender;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import java.util.List;
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
public class BotClientTimeoutTest {

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

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("app.communication.client.read-timeout", () -> Duration.ofSeconds(1));
        registry.add("resilience4j.retry.instances.bot.max-attempts", () -> 1);
        registry.add("app.bot.uri", () -> "http://localhost:54321");
        registry.add("resilience4j.circuitbreaker.instances.bot.minimum-number-of-calls", () -> 1);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
                "app.kafka.schema-registry",
                () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
        registry.add("app.redis.time-to-live", () -> "2s");
    }

    @Test
    public void TC1_1_ReturnTimeoutException_ThenShouldFallbackToKafka() {
        // given
        int fixedDelayMils = 3000;
        stubFor(post(urlEqualTo("/updates"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(fixedDelayMils)));

        LinkUpdate update = new LinkUpdate(1L, "https://example.com", "test", List.of(100L));

        // when
        long startTime = System.currentTimeMillis();
        botClient.sendUpdate(update);
        long endTime = System.currentTimeMillis();
        long executeTime = endTime - startTime;

        // then
        // Проверяем, что запрос прервался по таймауту, а не ждал 3 секунды
        assertThat(executeTime).isLessThan(fixedDelayMils);

        // Проверяем, что вызвался альтернативный транспорт
        verify(updateSender).sendUpdate(eq(update));
    }
}
