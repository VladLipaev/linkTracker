package backend.academy.linktracker.bot.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.AbstractIntegrationTest;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.config.TestBeans;
import backend.academy.linktracker.bot.dto.avro.AddLinkMessageAvro;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
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
public class ScrapperClientTimeoutTest extends AbstractIntegrationTest {

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ScrapperClient scrapperClient;

    @MockitoBean
    private TelegramUpdateService telegramUpdateService;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("app.communication.client.read-timeout", () -> Duration.ofSeconds(1));
        registry.add("resilience4j.retry.instances.scrapper.max-attempts", () -> 1);
        registry.add("app.scrapper.uri", () -> "http://localhost:54321");
        registry.add("resilience4j.circuitbreaker.instances.scrapper.minimum-number-of-calls", () -> 1);
    }

    @Test
    public void TC1_1_ReturnTimeoutException_ThenShouldFallbackToKafka() {
        // given
        long chatId = 100L;
        int fixedDelayMils = 3500;
        stubFor(post(urlEqualTo("/links"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(fixedDelayMils)));
        // when
        long startTime = System.currentTimeMillis();
        Throwable thrown = catchThrowable(() -> scrapperClient.addLink(chatId, "https://example.com", List.of("test")));
        long endTime = System.currentTimeMillis();
        long executeTime = endTime - startTime;

        // then
        assertThat(thrown)
                .isInstanceOf(ScrapperClientException.class)
                .hasMessageContaining("На стороне сервера проблемы, но вашу ссылку мы поставили в очередь");

        verify(kafkaTemplate).send(eq("link-add-topic"), any(AddLinkMessageAvro.class));
    }
}
