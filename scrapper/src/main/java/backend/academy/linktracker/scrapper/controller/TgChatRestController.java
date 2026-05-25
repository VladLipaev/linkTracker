package backend.academy.linktracker.scrapper.controller;

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
@RequestMapping("/tg-chat/{id}")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.communication.controller",
        name = "mode",
        havingValue = "rest",
        matchIfMissing = true)
public class TgChatRestController {

    private final TgChatService tgChatService;

    @PostMapping
    public ResponseEntity<Void> registerChat(@PathVariable("id") Long id) {
        tgChatService.addTgChat(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteChat(@PathVariable("id") Long id) {
        tgChatService.deleteTgChat(id);
        return ResponseEntity.ok().build();
    }
}
