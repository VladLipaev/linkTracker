package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.bot.TelegramBotRestClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HttpNotificationUpdateSender implements NotificationUpdateSender {

    private final TelegramBotRestClient botClient;

    @Override
    public void sendUpdate(LinkUpdate update) {
        botClient.sendUpdate(update);
    }
}
