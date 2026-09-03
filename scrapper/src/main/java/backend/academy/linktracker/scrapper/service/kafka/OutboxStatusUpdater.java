package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.repository.outbox.OutBoxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Deprecated
public class OutboxStatusUpdater {

    private final OutBoxRepository outBoxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "order.outbox",
        groupId = "linkupdate-outbox-status-updater",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void updateStatus(ConsumerRecord<String, String> record){
        try{
            JsonNode node = objectMapper.readTree(record.value());
            UUID eventId = UUID.fromString(node.get("id").asString());
            outBoxRepository.updateStatus(eventId, "published");

            log.info("Updated outbox event {}", eventId);

        }
        catch (Exception e){
            log.error("Failed to update outbox status", e);
        }
    }

}
