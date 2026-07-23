package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
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
import java.net.URI;
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
    private final ScrapperMetrics metrics;

    @Value("${app.controller.batch-size}")
    private Integer BATCH_SIZE;

    @Override
    @Transactional(readOnly = true)
    public ListLinksResponse getAllLinks(Long chatId, String tag) {
        Chat chat = tgChatRepository
                .findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Чат не зарегистрирован"));

        ListLinksResponse response;

        if (tag == null) {
            List<Link> links = chat.getLinks();
            if (links.isEmpty()) {
                response = ListLinksResponse.builder().links(List.of()).size(0).build();
            } else {
                List<LinkResponse> linkResponses = links.stream()
                        .map(link -> LinkResponse
                            .builder()
                            .id(link.getId())
                            .url(URI.create(link.getUrl()))
                            .tags(List.of()).build())
                        .toList();
                response = ListLinksResponse.builder().links(linkResponses).size(linkResponses.size()).build();
            }
        } else {
            List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsByChatIdAndTag(chatId, tag);
            List<LinkResponse> linkResponses = subscriptions.stream()
                    .map(sub -> LinkResponse.builder()
                            .id(sub.getLink().getId())
                            .url(URI.create(sub.getLink().getUrl()))
                                .tags(sub.getTags()
                            ).build())
                    .toList();
            response = ListLinksResponse.builder().links(linkResponses).size(linkResponses.size()).build();
        }

        return response;
    }

    @Override
    @Transactional
    public LinkResponse addLink(Long chatId, AddLinkRequest request) {

        Chat chat = tgChatRepository
                .findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Чат не зарегистрирован"));
        String url = String.valueOf(request.getLink());
        if (!linkValidator.isValid(url)) {
            throw new UnsupportedLinkException("Ссылка не поддерживается.");
        }

        if (linksRepository.findByChatIdAndUrl(chatId, url).isPresent()) {
            throw new LinkAlreadyExistsException("Ссылка %s для чата уже существует".formatted(String.valueOf(request.getLink())));
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
        List<String> tags = request.getTags();
        if (!tags.isEmpty()) {
            subscription.setTags(request.getTags());
        }
        subscriptionRepository.save(subscription);
        String domain = extractDomain(url);
        metrics.incrementLinks(domain);
        return LinkResponse.builder().id(savedLink.getId()).url(URI.create(savedLink.getUrl())).tags(tags).build();
    }

    @Override
    @Transactional
    public LinkResponse removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {

        if (tgChatRepository.findById(chatId).isEmpty()) {
            throw new ChatNotFoundException("Чат не зарегистрирован");
        }

        if (!linkValidator.isValid(String.valueOf(removeLinkRequest.getLink()))){
            throw new UnsupportedLinkException("Ссылка не поддерживается.");
        }
        Link link = linksRepository
                .findByChatIdAndUrl(chatId, String.valueOf(removeLinkRequest.getLink()))
                .orElseThrow(
                        () -> new NoSuchElementException("Cсылка %s не найдена".formatted(removeLinkRequest.getLink())));

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
        String domain = extractDomain(String.valueOf(removeLinkRequest.getLink()));
        metrics.decrementLinks(domain);
        return LinkResponse.builder().id(link.getId()).url(URI.create(link.getUrl())).tags(tags).build();
    }

    private String extractDomain(String url) {
        try {
            return new java.net.URI(url).getHost().replace("www.", "");
        } catch (Exception e) {
            return "unknown";
        }
    }
}
