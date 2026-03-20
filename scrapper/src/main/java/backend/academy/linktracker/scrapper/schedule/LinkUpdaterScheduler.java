package backend.academy.linktracker.scrapper.schedule;

import backend.academy.linktracker.scrapper.client.bot.TelegramBotRestClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.service.LinksService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkUpdaterScheduler {

    private final LinksRepository linksRepository;
    private final TelegramBotRestClient botClient;
    private final LinksService linksService;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void update() {
        log.atInfo()
                .setMessage("начало проверки на наличие обновлений в ссылках")
                .log();

        List<Link> links = linksRepository.findAll();

        if (links.isEmpty()) {
            log.atInfo().setMessage("проверка пропущена: ссылок не найдено").log();
            return;
        }

        for (Link link : links) {
            String url = link.getUrl();
            try {
                Optional<LinkUpdate> updateOpt = linksService.processLink(link);

                updateOpt.ifPresentOrElse(
                        update -> {
                            botClient.sendUpdate(update);
                            log.atInfo()
                                    .setMessage("Отправлено уведомление об обновлении")
                                    .addKeyValue("url", url)
                                    .log();
                        },
                        () -> log.atDebug()
                                .setMessage("Ссылка не обновлена")
                                .addKeyValue("url", url)
                                .log());

            } catch (Exception e) {
                log.atError()
                        .setMessage("Критическая ошибка при проверке ссылки")
                        .addKeyValue("url", url)
                        .setCause(e)
                        .log();
            }
        }
    }
}
