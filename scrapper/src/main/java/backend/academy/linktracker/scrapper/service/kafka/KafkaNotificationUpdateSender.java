package backend.academy.linktracker.scrapper.service.kafka;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.entity.OutBoxMessage;
import backend.academy.linktracker.scrapper.repository.outbox.OutBoxRepository;
import backend.academy.linktracker.scrapper.service.NotificationUpdateSender;
import jakarta.annotation.Priority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Priority(2)
@Slf4j
public class KafkaNotificationUpdateSender implements NotificationUpdateSender {

    private final OutBoxRepository outBoxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendUpdate(LinkUpdate update) {

        OutBoxMessage outBoxMessage =
                new OutBoxMessage(null, objectMapper.writeValueAsString(update), String.valueOf(update.id()));
        outBoxRepository.save(outBoxMessage);
        log.atInfo()
                .setMessage("Уведомление отправлено в outbox")
                .addKeyValue("url", update.url())
                .log();
    }
}
