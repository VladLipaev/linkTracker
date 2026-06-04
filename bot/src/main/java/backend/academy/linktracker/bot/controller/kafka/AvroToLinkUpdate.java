package backend.academy.linktracker.bot.controller.kafka;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.ProcessedLinkUpdateAvro;
import org.springframework.stereotype.Component;

@Component
public class AvroToLinkUpdate {

    public LinkUpdate avroToLinkUpdate(ProcessedLinkUpdateAvro linkUpdateAvro) {
        String description = linkUpdateAvro.getDescription();
        int newlineIndex = description.indexOf('\n');
        String url;
        if (newlineIndex != -1) {
            url = description.substring(0, newlineIndex).trim();
        } else {
            url = "unknown url";
        }
        return new LinkUpdate(
                linkUpdateAvro.getId(), url, linkUpdateAvro.getDescription(), linkUpdateAvro.getTgChatIds());
    }
}
