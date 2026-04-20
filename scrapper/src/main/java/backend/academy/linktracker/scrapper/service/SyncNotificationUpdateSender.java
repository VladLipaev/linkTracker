package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.bot.TelegramBotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.communication.client.mode}'.equals('rest') || '${app.communication.client.mode}'.equals('grpc')")
public class SyncNotificationUpdateSender implements NotificationUpdateSender {

    private final TelegramBotClient botClient;

    @Override
    public void sendUpdate(LinkUpdate update) {
        botClient.sendUpdate(update);
    }
}
