package backend.academy.linktracker.scrapper.config.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class ValkeyCscConfig {

    @Value("${app.redis.time-to-live}")
    private Duration ttl;

    @Bean
    public ChannelTopic cacheInvalidationTopic() {
        return new ChannelTopic("cache:invalidation:links");
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            CacheInvalidationListener cacheInvalidationListener,
            ChannelTopic cacheInvalidationTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(cacheInvalidationListener, cacheInvalidationTopic);
        return container;
    }

    @Bean
    public Cache<String, ListLinksResponse> localCache() {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfter(new Expiry<String, ListLinksResponse>() {
                @Override
                public long expireAfterCreate(String key, ListLinksResponse value, long currentTime) {
                    return ttl.toNanos();
                }

                @Override
                public long expireAfterUpdate(String key, ListLinksResponse value, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(String key, ListLinksResponse value, long currentTime, long currentDuration) {
                    return currentDuration;
                }

            })
            .build();
    }


}
