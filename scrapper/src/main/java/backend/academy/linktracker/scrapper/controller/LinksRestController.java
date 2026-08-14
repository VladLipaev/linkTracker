package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.api.LinksApi;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.ScrapperLinksService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.communication.controller",
        name = "mode",
        havingValue = "rest",
        matchIfMissing = true)
public class LinksRestController implements LinksApi {

    private final ScrapperLinksService linksService;

    @Override
    public ResponseEntity<LinkResponse> linksDelete(Long tgChatId, RemoveLinkRequest removeLinkRequest) {
        return ResponseEntity.ok(linksService.removeLink(tgChatId, removeLinkRequest));
    }

    @Override
    public ResponseEntity<ListLinksResponse> linksGet(Long tgChatId, String tag) {
        return ResponseEntity.ok(linksService.getLinks(tgChatId, tag));
    }

    @Override
    public ResponseEntity<LinkResponse> linksPost(Long tgChatId, AddLinkRequest addLinkRequest) {
        return ResponseEntity.ok(linksService.addLink(tgChatId, addLinkRequest));
    }
}
