package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.bot.TelegramBotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression(
        "'${app.communication.client.mode}'.equals('rest') || '${app.communication.client.mode}'.equals('grpc')")
@Slf4j
public class SyncNotificationUpdateSender implements NotificationUpdateSender {

    private final TelegramBotClient botClient;

    @Override
    public void sendUpdate(LinkUpdate update) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.atInfo()
                        .setMessage("Отправляем уведомление!")
                        .addKeyValue("url", update.url())
                        .log();
                botClient.sendUpdate(update);
            }
        });
    }
}
