package backend.academy.linktracker.scrapper.schedule;

import backend.academy.linktracker.scrapper.client.bot.TelegramBotRestClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.handler.LinkHandler;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkUpdaterScheduler {

    private final LinksRepository linksRepository;
    private final List<LinkHandler> linkHandlers;
    private final TelegramBotRestClient botClient;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void update() {
        log.atInfo()
                .setMessage("начало проверки на наличие обновлений в ссылках")
                .log();

        // 1. Получаем только уникальные URL из всей системы
        List<String> uniqueUrls = linksRepository.findAllUniqueUrls();

        if (uniqueUrls.isEmpty()) {
            log.atInfo().setMessage("проверка пропущена: ссылок не найдено").log();
            return;
        }

        // 2. Проверяем каждый уникальный URL
        for (String url : uniqueUrls) {
            try {
                processUrl(url);
            } catch (Exception e) {
                log.atError()
                        .setMessage("ошибка при проверке ссылки")
                        .addKeyValue("url", url)
                        .setCause(e)
                        .log();
            }
        }
    }

    private void processUrl(String url) {
        // 3. Ищем обработчик для данного типа ссылки (GitHub/SO)
        LinkHandler handler =
                linkHandlers.stream().filter(h -> h.supports(url)).findFirst().orElse(null);

        if (handler == null) {
            log.atWarn()
                    .setMessage("для ссылки не нашелся обработчик")
                    .addKeyValue("url", url)
                    .log();
            return;
        }

        // 4. Берем данные о ссылке из нашего хранилища (любую запись с этим URL)
        Link currentLink = linksRepository.getAnyLinkByUrl(url).orElseThrow();

        // 5. Запрашиваем внешнее API (GitHub/StackOverflow)
        try {
            OffsetDateTime lastUpdateFromApi = handler.fetchUpdate(url);

            // 6. Сравниваем даты
            if (lastUpdateFromApi.isAfter(currentLink.getLastUpdated())) {

                // 7. Находим ВСЕ чаты, которые подписаны на эту ссылку
                List<Long> chatIds = linksRepository.findAllChatIdsByUrl(url);

                log.atInfo()
                        .setMessage("обновление ссылки обнаружено")
                        .addKeyValue("url", url)
                        .addKeyValue("affected_chats_count", chatIds.size())
                        .addKeyValue("old_update_time", currentLink.getLastUpdated())
                        .addKeyValue("new_update_time", lastUpdateFromApi)
                        .log();

                // 8. Отправляем один запрос боту со списком всех ID
                botClient.sendUpdate(new LinkUpdate(
                        currentLink.getId(), url, "Новое обновление по ссылке на которую вы подписались", chatIds));

                // 9. Обновляем дату во всех записях с этим url (в памяти или бд)
                linksRepository.updateLastUpdatedByUrl(url, lastUpdateFromApi);
            }
        } catch (IllegalArgumentException e) {
            List<Long> chatIds = linksRepository.findAllChatIdsByUrl(url);
            botClient.sendUpdate(new LinkUpdate(currentLink.getId(), url, e.getMessage(), chatIds));
        }
    }
}
