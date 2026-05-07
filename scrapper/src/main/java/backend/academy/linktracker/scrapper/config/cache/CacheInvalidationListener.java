package backend.academy.linktracker.scrapper.config.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener implements MessageListener {

    private final Cache<String, ListLinksResponse> localCache;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String prefix = new String(message.getBody());
        log.atDebug()
                .setMessage("Получен сигнал броадкаста. Очищаем локальный кэш")
                .addKeyValue("prefix", prefix)
                .log();

        localCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }
}
