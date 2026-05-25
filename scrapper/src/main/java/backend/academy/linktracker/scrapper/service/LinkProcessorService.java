package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.GitHubClientException;
import backend.academy.linktracker.scrapper.client.StackOverflowException;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.handler.LinkHandler;
import backend.academy.linktracker.scrapper.handler.UpdateResult;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkProcessorService {

    private final List<LinkHandler> linkHandlers;
    private final LinksRepository linksRepository;
    private final SyncNotificationUpdateSender updateSender;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.client.batch-size}")
    private Integer BATCH_SIZE;

    public Optional<UpdateResult> processLink(Link link) {
        String url = link.getUrl();
        LinkHandler handler =
                linkHandlers.stream().filter(h -> h.supports(url)).findFirst().orElse(null);
        if (handler == null) {
            log.atWarn()
                    .setMessage("для ссылки не нашелся обработчик")
                    .addKeyValue("url", url)
                    .log();

            sendErrorNotification(link, "Невалидная ссылка %s".formatted(url));
            return Optional.empty();
        }

        try {
            return Optional.of(handler.fetchUpdate(url, link.getLastUpdated()));
        } catch (GitHubClientException | StackOverflowException e) {
            log.atWarn()
                    .setMessage("Ошибка внешнего API")
                    .addKeyValue("url", url)
                    .setCause(e)
                    .log();
            sendErrorNotification(link, "Временная ошибка при обращении к ресурсу: " + e.getMessage());
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.atError()
                    .setMessage("Ошибка обработки ссылки")
                    .addKeyValue("url", url)
                    .setCause(e)
                    .log();
            sendErrorNotification(link, "Формат ссылки некорректен" + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.atError()
                    .setMessage("Непредвиденная ошибка при проверке ресурса")
                    .addKeyValue("url", url)
                    .setCause(e)
                    .log();
            sendErrorNotification(link, "Внутренняя ошибка при проверке ресурса.");
            return Optional.empty();
        }
    }

    private void sendErrorNotification(Link link, String errorMessage) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Pageable pageable = PageRequest.of(0, BATCH_SIZE);
                Slice<Long> chatSlice = linksRepository.findAllChatIdsByUrl(link.getUrl(), pageable);
                if (chatSlice.hasContent()) {
                    updateSender.sendUpdate(
                            new LinkUpdate(link.getId(), link.getUrl(), errorMessage, chatSlice.getContent()));
                }
                while (chatSlice.hasNext()) {
                    chatSlice = linksRepository.findAllChatIdsByUrl(link.getUrl(), chatSlice.nextPageable());
                    updateSender.sendUpdate(
                            new LinkUpdate(link.getId(), link.getUrl(), errorMessage, chatSlice.getContent()));
                }
            });
        } catch (Exception ex) {
            log.atError()
                    .setMessage("Не удалось отправить уведомление об ошибке")
                    .addKeyValue("url", link.getUrl())
                    .setCause(ex)
                    .log();
        }
    }

    @Transactional
    public void handleUpdateResult(UpdateResult result, Link link) {
        String url = link.getUrl();

        if (result.newUpdatedAt().isAfter(link.getLastUpdated())) {
            linksRepository.updateLastUpdatedByUrl(url, result.newUpdatedAt());
        }

        if (result.hasUpdate()) {
            Pageable pageable = PageRequest.of(0, BATCH_SIZE);
            Slice<Long> chatSlice = linksRepository.findAllChatIdsByUrl(url, pageable);

            if (chatSlice.hasContent()) {
                updateSender.sendUpdate(
                        new LinkUpdate(link.getId(), url, result.description(), chatSlice.getContent()));
            }

            while (chatSlice.hasNext()) {
                chatSlice = linksRepository.findAllChatIdsByUrl(url, chatSlice.nextPageable());
                updateSender.sendUpdate(
                        new LinkUpdate(link.getId(), url, result.description(), chatSlice.getContent()));
            }
        }
    }

    @Transactional
    public void markBatchAsChecked(List<Long> linkIds, OffsetDateTime checkedAt) {
        if (!linkIds.isEmpty()) {
            linksRepository.updateLastCheckedAt(linkIds, checkedAt);
            log.atDebug()
                    .setMessage("Время проверки обновлено для ссылок")
                    .addKeyValue("size", linkIds.size())
                    .log();
        }
    }
}
