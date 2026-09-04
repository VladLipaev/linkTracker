package backend.academy.linktracker.bot.controller.cache;

import backend.academy.linktracker.bot.handler.dialog.UserSession;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheDialogUtil {

    private final Cache<String, UserSession> localCache;
    private final RedisTemplate<String, UserSession> redisTemplate;

    @Value("${app.redis.cache-key-prefix.dialog}")
    private String CACHE_KEY_PREFIX;

    @Value("${app.cache.dialog.use-redis:true}")
    private boolean useRedisConfig;

    private volatile boolean redisAvailable = true;

    private boolean isRedisAvailable() {
        if (!redisAvailable) {
            // Пытаемся восстановить соединение
            try {
                redisTemplate.getConnectionFactory().getConnection().ping();
                redisAvailable = true;
                log.info("Redis восстановлен, переключаемся в обычный режим");
                return true;
            } catch (Exception e) {
                log.atError()
                    .setMessage("Redis недоступен")
                        .addKeyValue("error.message", e.getMessage())
                    .log();
                return false;
            }
        }
        return true;
    }

    private void markRedisUnavailable(Exception e, String key) {
        if (redisAvailable) {
            redisAvailable = false;
            log.error("Redis недоступен, переключаемся на локальный кэш. Ошибка: {}", e.getMessage());
        }
    }

    public Optional<UserSession> getUserSession(Long chatId){
        String key = buildKey(chatId);
        try {
            UserSession local = localCache.getIfPresent(key);
            if (local != null) return Optional.of(local);
            if (useRedisConfig && isRedisAvailable()) {
                try {
                    UserSession cached = redisTemplate.opsForValue().get(key);
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
                    markRedisUnavailable(e, key);
                }
            }
            else{
                onlyLocalCacheAvailableLogging();
            }
        } catch (Exception e){
            log.error("Общая ошибка при получении из кэша", e);
        }
        return Optional.empty();
    }

    public void addCache(Long chatId, UserSession userSession, Duration ttl) {
        String key = buildKey(chatId);
        try {
            localCache.put(key, userSession);
            if (useRedisConfig && isRedisAvailable()){
                try{
                    redisTemplate.opsForValue().set(key, userSession, ttl);
                } catch (Exception e){
                    markRedisUnavailable(e, key);
                }
            }
            else{
                onlyLocalCacheAvailableLogging();
            }
        } catch (Exception e) {
            log.error("Ошибка при записи в кэш", e);
        }
    }

    private void onlyLocalCacheAvailableLogging() {
        log.debug("Redis недоступен, сохранено только в локальный кэш");
    }

    private String buildKey(Long chatId) {
        return CACHE_KEY_PREFIX + ":" + chatId;
    }


    public void invalidateChatCache(Long chatId) {
        String key = buildKey(chatId);
        try {
            localCache.invalidate(key);
            if (useRedisConfig && isRedisAvailable()) {
                try {
                    redisTemplate.delete(key);
                } catch (Exception e) {
                    markRedisUnavailable(e, key);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при инвалидации кэша", e);
        }
    }

}
