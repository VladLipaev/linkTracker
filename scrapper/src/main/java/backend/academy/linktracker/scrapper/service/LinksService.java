package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;

public interface LinksService {
    ListLinksResponse getAllLinks(Long chatId, String tag);

    LinkResponse addLink(Long chatId, AddLinkRequest addLinkRequest);

    LinkResponse removeLink(Long chatId, RemoveLinkRequest removeLinkRequest);
}
