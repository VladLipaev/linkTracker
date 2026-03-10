package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;

public interface TelegramBotRestClient {
    void sendUpdate(LinkUpdate linkUpdate);
}
