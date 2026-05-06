package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.LinkUpdateAvro;
import org.springframework.stereotype.Component;

@Component
public class LinkUpdateToAvroMapper {

    public LinkUpdateAvro linkUpdateAvro(LinkUpdate payload){
        return LinkUpdateAvro.newBuilder()
            .setId(payload.id())
            .setDescription(payload.description())
            .setUrl(payload.url())
            .setTgChatIds(payload.tgChatIds())
            .build();
    }

}
