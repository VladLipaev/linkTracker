package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.api.TgChatApi;
import backend.academy.linktracker.scrapper.dto.ChatAndListLinksResponse;
import backend.academy.linktracker.scrapper.service.TgChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.communication.controller",
        name = "mode",
        havingValue = "rest",
        matchIfMissing = true)
public class TgChatRestController implements TgChatApi {

    private final TgChatService tgChatService;

    @Override
    public ResponseEntity<Void> tgChatIdDelete(Long id) {
        tgChatService.deleteTgChat(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> tgChatIdPost(Long id) {
        tgChatService.addTgChat(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ChatAndListLinksResponse> tgChatIdGet(Long id) {
        return ResponseEntity.ok(tgChatService.getTgChatAndListLinks(id));
    }
}
