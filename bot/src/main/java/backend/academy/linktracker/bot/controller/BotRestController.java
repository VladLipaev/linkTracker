package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
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
    @RateLimiter(name = "api", fallbackMethod = "fallbackPostUpdate")
    public ResponseEntity<Void> postUpdate(@Valid @RequestBody LinkUpdate linkUpdate) {
        telegramUpdateService.postUpdate(linkUpdate);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> fallbackPostUpdate(LinkUpdate linkUpdate, RequestNotPermitted t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
