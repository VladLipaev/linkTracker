package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;

public interface TelegramBotRestClient {
    void sendUpdate(LinkUpdate linkUpdate);
}
