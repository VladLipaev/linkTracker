package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import backend.academy.linktracker.scrapper.handler.LinkValidator;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultLinksService implements LinksService {

    private final LinksRepository linksRepository;
    private final TgChatRepository tgChatRepository;
    private final LinkValidator linkValidator;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${app.controller.batch-size}")
    private Integer BATCH_SIZE;

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

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        Slice<Subscription> subscriptionSlice =
                subscriptionRepository.findSubscriptionsByChatIdAndTag(chatId, tag, pageable);
        List<Subscription> subscriptions = new ArrayList<>(subscriptionSlice.getContent());
        while (subscriptionSlice.hasNext()) {
            subscriptionSlice = subscriptionRepository.findSubscriptionsByChatIdAndTag(
                    chatId, tag, subscriptionSlice.nextPageable());
            subscriptions.addAll(subscriptionSlice.getContent());
        }
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

        if (linksRepository.findByChatIdAndUrl(chatId, url).isPresent()) {
            throw new LinkAlreadyExistsException("Ссылка %s для чата уже существует".formatted(request.link()));
        }
        Link savedLink;
        Optional<Link> existingLink = linksRepository.findByUrl(url);

        if (existingLink.isPresent()) {
            savedLink = existingLink.orElseThrow();
        } else {
            try {
                savedLink = linksRepository.save(new Link(url, OffsetDateTime.now()));
            } catch (DataIntegrityViolationException ex) {
                savedLink = linksRepository.findByUrl(url).orElseThrow();
            }
        }
        Subscription subscription = new Subscription(chatId, savedLink.getId());
        subscription.setChat(chat);
        subscription.setLink(savedLink);
        List<String> tags = request.tags();
        if (!tags.isEmpty()) {
            subscription.setTags(request.tags());
        }
        subscriptionRepository.save(subscription);
        return new LinkResponse(savedLink.getId(), savedLink.getUrl(), tags);
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
        Link link = linksRepository
                .findByChatIdAndUrl(chatId, removeLinkRequest.link())
                .orElseThrow(
                        () -> new NoSuchElementException("Cсылка %s не найдена".formatted(removeLinkRequest.link())));

        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        Slice<String> sliceTags = subscriptionRepository.findTagsByChatIdAndLinkId(chatId, link.getId(), pageable);
        List<String> tags = new ArrayList<>(sliceTags.getContent());
        while (sliceTags.hasNext()) {
            sliceTags =
                    subscriptionRepository.findTagsByChatIdAndLinkId(chatId, link.getId(), sliceTags.nextPageable());
            tags.addAll(sliceTags.getContent());
        }

        subscriptionRepository.deleteBySubscriptionId(new SubscriptionId(chatId, link.getId()));
        if (!subscriptionRepository.existsByLinkId(link.getId())) {
            linksRepository.deleteById(link.getId());
        }
        return new LinkResponse(link.getId(), link.getUrl(), tags);
    }
}
