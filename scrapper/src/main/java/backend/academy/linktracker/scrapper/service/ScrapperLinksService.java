package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperLinksService {
    private final LinksService linksService;
    private final CacheLinksUtil cacheLinksUtil;

    @Value("${app.redis.cache-key-prefix:links:byTgChat}")
    private String CACHE_KEY_PREFIX;

    @Value("${app.redis.time-to-live:2m}")
    private Duration ttl;


    public ListLinksResponse getLinks(Long chatId, String tag) {
        String key = buildCacheKey(chatId, tag);
        return cacheLinksUtil.getLinks(key)
                .orElseGet(() -> {
                    ListLinksResponse listLinksResponse = linksService.getAllLinks(chatId, tag);
                    cacheLinksUtil.addCache(key, listLinksResponse, ttl);
                    return listLinksResponse;
                });
    }

    public LinkResponse addLink(Long chatId, AddLinkRequest addLinkRequest) {
        cacheLinksUtil.invalidateChatCache(chatId);
        return linksService.addLink(chatId, addLinkRequest);
    }

    public LinkResponse removeLink(Long chatId, RemoveLinkRequest removeLinkRequest) {
        cacheLinksUtil.invalidateChatCache(chatId);
        return linksService.removeLink(chatId, removeLinkRequest);
    }

    private String buildCacheKey(Long chatId, String tag) {
        return String.format("%s::%s::%s", CACHE_KEY_PREFIX, chatId, tag != null ? tag : "all");
    }
}
