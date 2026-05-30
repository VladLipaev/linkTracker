package backend.academy.linktracker.ai.client.kafka;

import backend.academy.linktracker.ai.entity.dto.ProcessedLinkUpdateDto;
import backend.academy.linktracker.scrapper.dto.avro.ProcessedLinkUpdateAvro;
import org.springframework.stereotype.Component;

@Component
public class ProcessedLinkUpdateToAvroMapper {
    public ProcessedLinkUpdateAvro processedLinkUpdateAvro(ProcessedLinkUpdateDto message) {
        return ProcessedLinkUpdateAvro.newBuilder()
                .setId(message.getId())
                .setDescription(message.getDescription())
                .setTgChatIds(message.getTgChatIds())
                .setPriority(message.getPriority().name())
                .build();
    }
}
