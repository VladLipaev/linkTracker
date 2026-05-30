package backend.academy.linktracker.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.AbstractIntegrationTest;
import backend.academy.linktracker.ai.config.FilteringProperties;
import backend.academy.linktracker.ai.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.avro.ProcessedLinkUpdateAvro;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(TestBeans.class)
@SpringBootTest
@Testcontainers
public class KafkaProcessingIT extends AbstractIntegrationTest {

    @MockitoBean
    private YandexGPTRestClient yandexGPTRestClient;

    @Autowired
    private FilteringProperties filteringProperties;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.yandexgpt.folder-id", () -> "dummy-folder-id");
        registry.add("app.yandexgpt.api-key", () -> "dummy-api-key");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("app.grouping.window-ms", () -> 1000);
        registry.add("app.kafka.producer.send-delay", () -> Duration.ofMillis(2000));
    }

    private Consumer<String, ProcessedLinkUpdateAvro> processedConsumer;
    private Producer<String, RawLinkUpdateAvro> rawProducer;

    private final String rawTopicName = "link.raw-updates";
    private final String processedTopicName = "link.processed-updates";

    @BeforeEach
    void setUp() {
        // Создаем продюсера для raw-топика
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, AbstractIntegrationTest.KAFKA_CONTAINER.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        producerProps.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, getSchemaRegistryUrl());
        rawProducer = new org.apache.kafka.clients.producer.KafkaProducer<>(producerProps);

        // Создаем консьюмера для processed-топика
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, AbstractIntegrationTest.KAFKA_CONTAINER.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-processed-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, getSchemaRegistryUrl());
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        processedConsumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
        processedConsumer.subscribe(Collections.singletonList(processedTopicName));
    }

    @AfterEach
    void tearDown() {
        rawProducer.close();
        processedConsumer.close();
    }

    @Test
    @DisplayName("TC-3.1: Успешная публикация обработанного сообщения в output topic")
    void shouldPublishProcessedMessageToOutputTopic() {
        // Arrange
        String validResponseJson = "{\"result\":{\"alternatives\":[{\"message\":{\"text\":\"Mocked summary\"}}]}}";
        when(yandexGPTRestClient.gptRequest(any())).thenReturn(validResponseJson);

        // Создаем сообщение, которое точно пройдет валидацию
        String longDescription = "http://example.com/update\n" + "Critical security vulnerability fixed in production. "
                + "This update addresses multiple security issues found in the latest audit. "
                + "A".repeat(400); // Убедимся что длина > min-length (250) и > threshold (500)

        RawLinkUpdateAvro validMessage = RawLinkUpdateAvro.newBuilder()
                .setId(System.currentTimeMillis())
                .setAuthor("valid_author")
                .setDescription(longDescription)
                .setTgChatIds(List.of(1001L))
                .build();

        ProducerRecord<String, RawLinkUpdateAvro> record = new ProducerRecord<>(rawTopicName, "key-1", validMessage);
        record.headers().add("event-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        // Act
        rawProducer.send(record);
        rawProducer.flush();

        // Assert
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, ProcessedLinkUpdateAvro> records = processedConsumer.poll(Duration.ofMillis(1000));

            assertThat(records.count()).isGreaterThan(0);

            ProcessedLinkUpdateAvro processedMessage = records.iterator().next().value();
            assertThat(processedMessage.getPriority()).isEqualTo("HIGH");
            assertThat(processedMessage.getTgChatIds()).contains(1001L);
            assertThat(processedMessage.getDescription()).contains("Mocked summary");
            assertThat(processedMessage.getDescription()).contains("http://example.com/update");
        });
    }

    @Test
    @DisplayName("TC-3.2: Отсутствие публикации отфильтрованного сообщения")
    void shouldNotPublishFilteredMessage() {
        // Arrange - сообщение с исключенным автором
        RawLinkUpdateAvro filteredMessage = RawLinkUpdateAvro.newBuilder()
                .setId(System.currentTimeMillis() + 1)
                .setAuthor("self") // исключенный автор
                .setDescription("Some update that should be filtered out because author is excluded")
                .setTgChatIds(List.of(1002L))
                .build();

        ProducerRecord<String, RawLinkUpdateAvro> record = new ProducerRecord<>(rawTopicName, "key-2", filteredMessage);
        record.headers().add("event-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        // Act
        rawProducer.send(record);
        rawProducer.flush();

        // Assert - проверяем, что сообщение НЕ появилось в выходном топике
        await().pollDelay(3, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, ProcessedLinkUpdateAvro> records = processedConsumer.poll(Duration.ofMillis(1000));

            boolean hasFilteredMessage = false;
            records.forEach(r -> {
                if (r.value().getId() == filteredMessage.getId()) {
                    throw new AssertionError("Filtered message should not appear in output topic");
                }
            });

            // Просто проверяем, что нет сообщений с filtered id
            assertThat(records.count()).isZero();
        });
    }

    @Test
    @DisplayName("TC-3.1.2: Сообщение без ключевых слов -> MEDIUM приоритет")
    void shouldAssignMediumPriorityToMessageWithoutKeywords() {
        // Arrange
        String description = "http://example.com/update\n" + "B".repeat(300);

        RawLinkUpdateAvro message = RawLinkUpdateAvro.newBuilder()
                .setId(System.currentTimeMillis() + 2)
                .setAuthor("regular_developer")
                .setDescription(description)
                .setTgChatIds(List.of(1003L))
                .build();

        ProducerRecord<String, RawLinkUpdateAvro> record = new ProducerRecord<>(rawTopicName, "key-3", message);
        record.headers().add("event-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        // Act
        rawProducer.send(record);
        rawProducer.flush();

        // Assert
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, ProcessedLinkUpdateAvro> records = processedConsumer.poll(Duration.ofMillis(1000));

            assertThat(records.count()).isGreaterThan(0);
            ProcessedLinkUpdateAvro processedMessage = records.iterator().next().value();
            assertThat(processedMessage.getPriority()).isEqualTo("MEDIUM");
        });
    }

    private String getSchemaRegistryUrl() {
        return "http://" + AbstractIntegrationTest.SCHEMA_REGISTRY.getHost() + ":"
                + AbstractIntegrationTest.SCHEMA_REGISTRY.getFirstMappedPort();
    }
}
