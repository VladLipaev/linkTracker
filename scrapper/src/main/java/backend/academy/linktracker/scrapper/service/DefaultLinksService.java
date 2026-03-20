package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import backend.academy.linktracker.scrapper.handler.LinkHandler;
import backend.academy.linktracker.scrapper.handler.LinkValidator;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultLinksService implements LinksService {

    private final LinksRepository LinksRepository;
    private final TgChatRepository tgChatRepository;
    private final LinkValidator linkValidator;
    private final List<LinkHandler> linkHandlers;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public ListLinksResponse getAllLinks(Long chatId, String tag) {
        Chat chat = tgChatRepository
                .findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Чат не зарегистрирован"));
        List<Link> links;

        if (tag == null) {
            links = chat.getLinks();
            if (links.isEmpty()) {
                return new ListLinksResponse(List.of(), 0);
            }
            List<LinkResponse> linkResponses = links.stream()
                    .map(link -> new LinkResponse(link.getId(), link.getUrl(), List.of()))
                    .toList();
            return new ListLinksResponse(linkResponses, linkResponses.size());
        }
        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsByChatIdAndTag(chatId, tag);
        List<LinkResponse> linkResponses = subscriptions.stream()
                .map(sub ->
                        new LinkResponse(sub.getLink().getId(), sub.getLink().getUrl(), sub.getTags()))
                .toList();

        return new ListLinksResponse(linkResponses, linkResponses.size());
    }

    @Override
    @Transactional
    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        Chat chat = tgChatRepository
                .findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Чат не зарегистрирован"));
        String url = request.link();
        if (!linkValidator.isValid(url)) {
            throw new UnsupportedLinkException("Ссылка не поддерживается.");
        }

        if (LinksRepository.findByChatIdAndUrl(chatId, url).isPresent()) {
            throw new LinkAlreadyExistsException("Ссылка %s для чата уже существует".formatted(request.link()));
        }
        Link saved;
        Optional<Link> link = LinksRepository.findByUrl(url);
        saved = link.orElse(LinksRepository.save(new Link(url, OffsetDateTime.now())));
        Subscription subscription = new Subscription(chatId, saved.getId());
        subscription.setChat(chat);
        subscription.setLink(saved);
        List<String> tags = request.tags();
        if (!tags.isEmpty()) {
            subscription.setTags(request.tags());
        }
        subscriptionRepository.save(subscription);
        return new LinkResponse(saved.getId(), saved.getUrl(), tags);
    }

    @Override
    @Transactional
    public LinkResponse removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        if (tgChatRepository.findById(chatId).isEmpty()) {
            throw new ChatNotFoundException("Чат не зарегистрирован");
        }

        if (!linkValidator.isValid(removeLinkRequest.link())) {
            throw new UnsupportedLinkException("Ссылка не поддерживается.");
        }
        Link link = LinksRepository.findByChatIdAndUrl(chatId, removeLinkRequest.link())
                .orElseThrow(
                        () -> new NoSuchElementException("Cсылка %s не найдена".formatted(removeLinkRequest.link())));

        List<String> tags = subscriptionRepository.findTagsByChatIdAndLinkId(chatId, link.getId());

        subscriptionRepository.deleteBySubscriptionId(new SubscriptionId(chatId, link.getId()));
        if (!subscriptionRepository.existsByLinkId(link.getId())) {
            LinksRepository.deleteById(link.getId());
        }
        return new LinkResponse(link.getId(), link.getUrl(), tags);
    }

    @Override
    @Transactional
    public Optional<LinkUpdate> processLink(Link link) {
        String url = link.getUrl();
        LinkHandler handler =
                linkHandlers.stream().filter(h -> h.supports(url)).findFirst().orElse(null);

        if (handler == null) {
            log.atError()
                    .setMessage("для ссылки не нашелся обработчик")
                    .addKeyValue("url", url)
                    .log();
            throw new IllegalArgumentException("Невалидная ссылка %s".formatted(url));
        }

        try {
            OffsetDateTime lastUpdateFromApi = handler.fetchUpdate(url);

            if (lastUpdateFromApi.isAfter(link.getLastUpdated())) {

                List<Long> chatIds = LinksRepository.findAllChatIdsByUrl(url);

                log.atInfo()
                        .setMessage("обновление ссылки обнаружено")
                        .addKeyValue("url", url)
                        .addKeyValue("affected_chats_count", chatIds.size())
                        .addKeyValue("old_update_time", link.getLastUpdated())
                        .addKeyValue("new_update_time", lastUpdateFromApi)
                        .log();

                LinksRepository.updateLastUpdatedByUrl(url, lastUpdateFromApi);

                return Optional.of(new LinkUpdate(
                        link.getId(), url, "Новое обновление по ссылке на которую вы подписались", chatIds));
            }
        } catch (IllegalArgumentException e) {
            List<Long> chatIds = LinksRepository.findAllChatIdsByUrl(url);
            return Optional.of(new LinkUpdate(link.getId(), url, e.getMessage(), chatIds));
        }
        return Optional.empty();
    }
}
