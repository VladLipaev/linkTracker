package backend.academy.linktracker.bot.controller.kafka;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.LinkUpdateAvro;
import org.springframework.stereotype.Component;

@Component
public class AvroToLinkUpdate {

    public LinkUpdate avroToLinkUpdate(LinkUpdateAvro linkUpdateAvro) {
        return new LinkUpdate(
                linkUpdateAvro.getId(),
                linkUpdateAvro.getUrl().toString(),
                linkUpdateAvro.getDescription().toString(),
                linkUpdateAvro.getTgChatIds());
    }
}
