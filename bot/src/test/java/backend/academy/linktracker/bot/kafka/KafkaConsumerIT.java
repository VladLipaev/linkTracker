package backend.academy.linktracker.bot.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.AbstractIntegrationTest;
import backend.academy.linktracker.bot.config.TestBeans;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import backend.academy.linktracker.scrapper.dto.avro.LinkUpdateAvro;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Import(TestBeans.class)
@Testcontainers
public class KafkaConsumerIT extends AbstractIntegrationTest {
    // Мокаем сервис, чтобы не слать реальные запросы в Telegram API и проверять вызовы
    @MockitoBean
    private TelegramUpdateService telegramUpdateService;

    private static final String TOPIC = "link-updates-topic";

    @MockitoSpyBean
    private TestBeans.DlqListenerWatcher dlqListenerWatcher;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Отправка валидного сообщения -> успешная обработка")
    void shouldProcessValidMessageSuccessfully() {
        // 1. Подготовка валидного сообщения
        UUID eventId = UUID.randomUUID();
        LinkUpdateAvro validAvro = LinkUpdateAvro.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/test/repo")
                .setDescription("Новый коммит")
                .setTgChatIds(List.of(123L, 456L))
                .build();

        // Отправляем с заголовком event-id, так как консьюмер его ждет
        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, null, validAvro);
        record.headers().add("event-id", eventId.toString().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);

        verify(telegramUpdateService, timeout(5000)).postUpdate(any(LinkUpdate.class));
    }

    @Test
    @DisplayName("Отправка невалидного сообщения -> валидация падает -> улетает в DLQ")
    void shouldSendToDlqOnValidationFailure() throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        LinkUpdateAvro invalidAvro = LinkUpdateAvro.newBuilder()
                .setId(1L)
                .setUrl("") // Спровоцирует ошибку валидации
                .setDescription("Новый коммит")
                .setTgChatIds(List.of())
                .build();

        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, null, invalidAvro);
        record.headers().add("event-id", eventId.toString().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record);

        verify(dlqListenerWatcher, timeout(5000)).listenDlq(any(ConsumerRecord.class));
    }

    @Test
    @DisplayName("Ошибка сети (ResourceAccessException) -> ретраи -> в итоге DLQ (или успех)")
    void shouldRetryOnResourceAccessException() throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        LinkUpdateAvro validAvro = LinkUpdateAvro.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/test/repo")
                .setDescription("Новый коммит")
                .setTgChatIds(List.of(123L))
                .build();

        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, null, validAvro);
        record.headers().add("event-id", eventId.toString().getBytes(StandardCharsets.UTF_8));

        // Симулируем падение Telegram API
        doThrow(new ResourceAccessException("Telegram API is down"))
                .when(telegramUpdateService)
                .postUpdate(any(LinkUpdate.class));

        kafkaTemplate.send(record);

        verify(telegramUpdateService, timeout(10000).times(3)).postUpdate(any(LinkUpdate.class));

        // Проверяем, что после исчерпания ретраев сообщение упало в DLQ
        verify(dlqListenerWatcher, timeout(5000)).listenDlq(any(ConsumerRecord.class));
    }
}
