package backend.academy.linktracker.ai.controller.kafka;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.AbstractIntegrationTest;
import backend.academy.linktracker.ai.client.YandexGPTRestClient;
import backend.academy.linktracker.ai.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Import(TestBeans.class)
public class RawLinkUpdateConsumerIT extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.yandexgpt.folder-id", () -> "dummy-folder-id");
        registry.add("app.yandexgpt.api-key", () -> "dummy-api-key");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Autowired
    @Qualifier("botDlqKafkaTemplate")
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.consumer.topic.name}")
    private String topicName;

    @MockitoBean
    private YandexGPTRestClient yandexGPTRestClient;

    @Test
    @DisplayName("TC-1.1: Получение и обработка корректного сообщения")
    void shouldProcessValidMessageSuccessfully() {
        // Arrange
        String validResponseJson = "{\"result\":{\"alternatives\":[{\"message\":{\"text\":\"Mocked summary\"}}]}}";
        when(yandexGPTRestClient.gptRequest(any())).thenReturn(validResponseJson);

        String longDescription = "a".repeat(505);

        RawLinkUpdateAvro validMessage = RawLinkUpdateAvro.newBuilder()
                .setId(123L)
                .setAuthor("valid_author")
                .setDescription(longDescription)
                .setTgChatIds(List.of(1001L, 1002L))
                .build();

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topicName, String.valueOf(validMessage.getId()), validMessage);
        record.headers().add("event-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        // Act
        try {
            kafkaTemplate.send(record).get();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при отправке сообщения в Kafka", e);
        }

        // Assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(yandexGPTRestClient, times(1)).gptRequest(any());
        });
    }

    @Test
    @DisplayName("TC-1.2: Обработка некорректного сообщения без падения (отсутствует event-id)")
    void shouldHandleInvalidMessageWithoutCrashing() {
        // Arrange
        RawLinkUpdateAvro invalidMessage = RawLinkUpdateAvro.newBuilder()
                .setId(456L)
                .setAuthor("valid_author")
                .setDescription("a".repeat(505))
                .setTgChatIds(List.of(2001L))
                .build();

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topicName, String.valueOf(invalidMessage.getId()), invalidMessage);

        // Act (отправляем без event-id)
        kafkaTemplate.send(record);

        // Assert
        await().pollDelay(Duration.ofSeconds(3)).untilAsserted(() -> {
            verify(yandexGPTRestClient, never()).gptRequest(any());
        });
    }
}
