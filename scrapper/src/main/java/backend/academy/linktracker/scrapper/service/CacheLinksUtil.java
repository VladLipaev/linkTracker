package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import com.github.benmanes.caffeine.cache.Cache;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheLinksUtil {

    private final RedisTemplate<String, ListLinksResponse> redisTemplate;
    private final Cache<String, ListLinksResponse> localCache;

    @Value("${app.redis.cache-key-prefix:links:byTgChat}")
    private String CACHE_KEY_PREFIX;

    public Optional<ListLinksResponse> getLinks(String key) {
        try {
            ListLinksResponse local = localCache.getIfPresent(key);
            if (local != null) return Optional.of(local);
            ListLinksResponse cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                Long expireSecs = redisTemplate.getExpire(key, TimeUnit.SECONDS);

                if (expireSecs != null && expireSecs > 0) {
                    localCache
                            .policy()
                            .expireVariably()
                            .ifPresent(policy -> policy.put(key, cached, expireSecs, TimeUnit.SECONDS));
                } else if (expireSecs != null && expireSecs == -1) {
                    localCache.put(key, cached);
                }
                return Optional.of(cached);
            }
        } catch (Exception e) {
            log.atError()
                    .setMessage("кэш отвалился")
                    .setCause(e)
                    .addKeyValue("error.message", e.getMessage())
                    .log();
        }
        return Optional.empty();
    }

    public void addCache(String key, ListLinksResponse listLinksResponse, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, listLinksResponse, ttl);
            localCache.put(key, listLinksResponse);
        } catch (Exception e) {
            log.atError()
                    .setMessage("ошибка кэширования")
                    .setCause(e)
                    .addKeyValue("valkey.key", key)
                    .log();
        }
    }

    public void invalidateChatCache(Long chatId) {
        String prefix = String.format("%s::%s::", CACHE_KEY_PREFIX, chatId);
        String searchPattern = String.format("%s::%s::%s", CACHE_KEY_PREFIX, chatId, "*");
        try {
            Set<String> keysToDelete = redisTemplate.keys(searchPattern);
            if (keysToDelete != null && !keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                localCache.invalidateAll(keysToDelete);
            }
        } catch (Exception e) {
            log.atError()
                    .setMessage("Ошибка при сбросе кэша для чата")
                    .addKeyValue("valkey.chatId.prefix", prefix)
                    .setCause(e)
                    .log();
        }
    }
}
