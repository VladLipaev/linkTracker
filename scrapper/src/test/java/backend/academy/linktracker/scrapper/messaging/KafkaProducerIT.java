package backend.academy.linktracker.scrapper.messaging;

import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.KAFKA_CONTAINER;
import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.SCHEMA_REGISTRY;
import static backend.academy.linktracker.scrapper.config.TestBeans.POSTGRES;
import static org.assertj.core.api.Assertions.assertThat; // Обрати внимание, импорт изменен на общий Assertions
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.LinkUpdateAvro;
import backend.academy.linktracker.scrapper.entity.OutBoxMessage;
import backend.academy.linktracker.scrapper.repository.outbox.OutBoxRepository;
import backend.academy.linktracker.scrapper.service.NotificationUpdateSender;
import backend.academy.linktracker.scrapper.service.kafka.KafkaOutboxBatcher;
import backend.academy.linktracker.scrapper.service.kafka.KafkaOutboxWorker;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Import(TestBeans.class)
public class KafkaProducerIT {

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
    }

    @Autowired
    private KafkaOutboxBatcher kafkaOutboxBatcher;

    @Autowired
    private KafkaOutboxWorker kafkaOutboxWorker;

    @Autowired
    private NewTopic newTopic;

    @Autowired
    private OutBoxRepository outBoxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private LinkUpdate linkUpdate;

    @MockitoBean
    private KafkaTemplate<String, LinkUpdateAvro> kafkaTemplate;

    @Autowired
    private NotificationUpdateSender notificationUpdateSender;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
            "app.kafka.schema-registry",
            () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
    }

    @BeforeEach
    void setUp() {
        linkUpdate = new LinkUpdate(1L, "https://github.com/SasniyKunchik/Faceofgeneration", "lalala", List.of(1L));
    }

    @AfterEach
    void cleanUp() {
        outBoxRepository.deleteAll();
    }

    @Test
    void producerSendMessage_topicAndMessageStructureShouldBeAsExpected() {
        OutBoxMessage outBoxMessage = new OutBoxMessage(
            UUID.randomUUID(), objectMapper.writeValueAsString(linkUpdate), String.valueOf(linkUpdate.id()));
        outBoxRepository.save(outBoxMessage);

        SendResult<String, LinkUpdateAvro> dummyResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, LinkUpdateAvro>> future = CompletableFuture.completedFuture(dummyResult);

        when(kafkaTemplate.send(Mockito.any(ProducerRecord.class))).thenReturn(future);

        kafkaOutboxWorker.sendToKafka();

        ArgumentCaptor<ProducerRecord<String, LinkUpdateAvro>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, LinkUpdateAvro> record = captor.getValue();
        assertThat(record.topic()).isEqualTo(newTopic.name());
        assertThat(record.value().get("id")).isEqualTo(linkUpdate.id());
        assertThat(record.value().get("url")).isEqualTo(linkUpdate.url());
        assertThat(record.value().get("description")).isEqualTo(linkUpdate.description());
        assertThat(record.value().get("tgChatIds")).isEqualTo(linkUpdate.tgChatIds());
    }

    @Test
    void updateToOutbox_shouldSaveToOutboxTable() {
        notificationUpdateSender.sendUpdate(linkUpdate);
        Optional<OutBoxMessage> outBoxMessageTakenOptional =
            outBoxRepository.findByPartitionKey(String.valueOf(linkUpdate.id()));

        assertThat(outBoxMessageTakenOptional).isPresent();

        OutBoxMessage outBoxMessageTaken = outBoxMessageTakenOptional.get();
        LinkUpdate linkUpdateTaken = objectMapper.readValue(outBoxMessageTaken.getPayload(), LinkUpdate.class);

        assertThat(linkUpdateTaken.id()).isEqualTo(linkUpdate.id());
        assertThat(linkUpdateTaken.url()).isEqualTo(linkUpdate.url());
        assertThat(linkUpdateTaken.description()).isEqualTo(linkUpdate.description());
        assertThat(linkUpdateTaken.tgChatIds()).isEqualTo(linkUpdate.tgChatIds());
    }

    @Test
    void sendUpdate_valid_shouldChangeStatusOfOutboxMessageToSent() {
        notificationUpdateSender.sendUpdate(linkUpdate);
        Optional<OutBoxMessage> outBoxMessageOptional1 =
            outBoxRepository.findByPartitionKey(linkUpdate.id().toString());

        assertThat(outBoxMessageOptional1).isPresent();
        OutBoxMessage outBoxMessage1 = outBoxMessageOptional1.get();
        assertThat(outBoxMessage1.getStatus()).isEqualTo("new");

        ProducerRecord<String, LinkUpdateAvro> producerRecord = mock(ProducerRecord.class);
        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(kafkaTemplate.send(Mockito.any(ProducerRecord.class)))
            .thenReturn(CompletableFuture.completedFuture(new SendResult<>(producerRecord, recordMetadata)));

        kafkaOutboxWorker.sendToKafka();

        Optional<OutBoxMessage> outBoxMessageOptional2 =
            outBoxRepository.findByPartitionKey(linkUpdate.id().toString());

        assertThat(outBoxMessageOptional2).isPresent();
        OutBoxMessage outBoxMessage2 = outBoxMessageOptional2.get();
        assertThat(outBoxMessage2.getStatus()).isEqualTo("sent");
    }

    @Test
    void sendUpdate_inValid_shouldChangeStatusOfOutboxMessageToError() {
        notificationUpdateSender.sendUpdate(linkUpdate);
        Optional<OutBoxMessage> outBoxMessageOptional1 =
            outBoxRepository.findByPartitionKey(linkUpdate.id().toString());

        assertThat(outBoxMessageOptional1).isPresent();
        OutBoxMessage outBoxMessage1 = outBoxMessageOptional1.get();
        assertThat(outBoxMessage1.getStatus()).isEqualTo("new");

        CompletableFuture<SendResult<String, LinkUpdateAvro>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalArgumentException("record cannot be null"));

        when(kafkaTemplate.send(Mockito.any(ProducerRecord.class))).thenReturn(failedFuture);

        kafkaOutboxWorker.sendToKafka();

        Optional<OutBoxMessage> outBoxMessageOptional2 =
            outBoxRepository.findByPartitionKey(linkUpdate.id().toString());

        assertThat(outBoxMessageOptional2).isPresent();
        OutBoxMessage outBoxMessage2 = outBoxMessageOptional2.get();
        assertThat(outBoxMessage2.getStatus()).isEqualTo("error");
    }
}
