package backend.academy.linktracker.scrapper.schedule;

import backend.academy.linktracker.scrapper.service.kafka.KafkaOutboxBatcher;
import backend.academy.linktracker.scrapper.service.kafka.KafkaOutboxWorker;
import backend.academy.linktracker.scrapper.service.kafka.KafkaTemplateConfiguration;
import backend.academy.linktracker.scrapper.service.kafka.KafkaTopicConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class KafkaLinkUpdaterSchedulerIT extends BaseLinkUpdaterSchedulerIT {

    @MockitoBean
    private KafkaOutboxBatcher kafkaOutboxBatcher;

    @MockitoBean
    private KafkaOutboxWorker kafkaOutboxWorker;

    @MockitoBean
    private KafkaTopicConfiguration kafkaTopicConfiguration;

    @MockitoBean
    private KafkaTemplateConfiguration kafkaTemplateConfiguration;
}
