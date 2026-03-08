package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.handler.LinkValidator;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultLinksService implements LinksService {

    private final LinksRepository linksRepository;
    private final TgChatRepository tgChatRepository;
    private final LinkValidator linkValidator;

    @Override
    public ListLinksResponse getAllLinks(Long chatId, String tag) {
        if (tgChatRepository.findByChatId(chatId).isEmpty()) {
            throw new ChatNotFoundException("Чат с ID " + chatId + " не зарегистрирован");
        }

        List<Link> links = tag != null
                ? linksRepository.findAllByChatIdAndTag(chatId, tag)
                : linksRepository.findAllByChatId(chatId);

        if (links == null) {
            links = List.of();
        }

        List<LinkResponse> linkResponses = links.stream()
                .map(link -> new LinkResponse(link.getId(), link.getUrl(), link.getTags()))
                .toList();

        return new ListLinksResponse(linkResponses, linkResponses.size());
    }

    @Override
    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        if (tgChatRepository.findByChatId(chatId).isEmpty()) {
            throw new ChatNotFoundException("Чат с ID %d не зарегистрирован".formatted(chatId));
        }

        if (!linkValidator.isValid(request.link())) {
            throw new UnsupportedLinkException("Ссылка не поддерживается.");
        }

        if (linksRepository.findByChatIdAndUrl(chatId, request.link()).isPresent()) {
            throw new LinkAlreadyExistsException(
                    "Ссылка %s для чата %d уже существует".formatted(request.link(), chatId));
        }

        Link link = new Link(null, chatId, request.link(), request.tags(), OffsetDateTime.now());
        Link saved = linksRepository.save(link);
        return new LinkResponse(saved.getId(), saved.getUrl(), saved.getTags());
    }

    @Override
    public LinkResponse removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        if (tgChatRepository.findByChatId(chatId).isEmpty()) {
            throw new ChatNotFoundException("Чат с ID %d не зарегистрирован".formatted(chatId));
        }

        if (!linkValidator.isValid(removeLinkRequest.link())) {
            throw new UnsupportedLinkException("Ссылка не поддерживается.");
        }

        Link link = linksRepository
                .deleteByChatIdAndUrl(chatId, removeLinkRequest.link())
                .orElseThrow(
                        () -> new NoSuchElementException("Cсылка %s не найдена".formatted(removeLinkRequest.link())));

        return new LinkResponse(link.getId(), link.getUrl(), link.getTags());
    }
}
