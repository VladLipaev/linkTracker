package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.ScrapperLinksService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.communication.controller",
        name = "mode",
        havingValue = "rest",
        matchIfMissing = true)
public class LinksRestController {

    private final ScrapperLinksService linksService;

    @GetMapping
    @RateLimiter(name = "api", fallbackMethod = "fallbackGetAllLinks")
    public ResponseEntity<?> getAllLinks(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestParam(value = "tag", required = false) String tag) {
        return ResponseEntity.ok().body(linksService.getLinks(chatId, tag));
    }

    public ResponseEntity<?> fallbackGetAllLinks(Long chatId, String tag, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @PostMapping
    @RateLimiter(name = "api", fallbackMethod = "fallbackAddLink")
    public ResponseEntity<?> addLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @Valid @RequestBody AddLinkRequest addLinkRequest) {
        return ResponseEntity.ok(this.linksService.addLink(chatId, addLinkRequest));
    }

    public ResponseEntity<?> fallbackAddLink(Long chatId, AddLinkRequest addLinkRequest, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    @DeleteMapping
    @RateLimiter(name = "api", fallbackMethod = "fallbackRemoveLink")
    public ResponseEntity<?> removeLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @Valid @RequestBody RemoveLinkRequest removeLinkRequest) {
        return ResponseEntity.ok(this.linksService.removeLink(chatId, removeLinkRequest));
    }

    public ResponseEntity<?> fallbackRemoveLink(Long chatId, RemoveLinkRequest removeLinkRequest, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
}
