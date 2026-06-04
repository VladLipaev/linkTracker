package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/updates")
@ConditionalOnProperty(prefix = "app.communication.controller", name = "mode", havingValue = "rest")
public class BotRestController {

    private final TelegramUpdateService telegramUpdateService;

    @PostMapping
    public ResponseEntity<Void> postUpdate(@Valid @RequestBody LinkUpdate linkUpdate) {
        telegramUpdateService.postUpdate(linkUpdate);
        return ResponseEntity.ok().build();
    }
}
