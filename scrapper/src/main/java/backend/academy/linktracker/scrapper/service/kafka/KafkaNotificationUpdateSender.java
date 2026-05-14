package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.entity.OutBoxMessage;
import backend.academy.linktracker.scrapper.repository.outbox.OutBoxRepository;
import backend.academy.linktracker.scrapper.service.NotificationUpdateSender;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.communication.client", name = "mode", havingValue = "kafka", matchIfMissing = true)
public class KafkaNotificationUpdateSender implements NotificationUpdateSender {

    private final OutBoxRepository outBoxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void sendUpdate(LinkUpdate update) {

        OutBoxMessage outBoxMessage = new OutBoxMessage(
                UUID.randomUUID(), objectMapper.writeValueAsString(update), String.valueOf(update.id()));
        outBoxRepository.save(outBoxMessage);
        log.atInfo()
                .setMessage("Уведомление отправлено в outbox")
                .addKeyValue("url", update.url())
                .log();
    }
}
