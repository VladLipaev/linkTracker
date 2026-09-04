package backend.academy.linktracker.bot.controller.cache;

import backend.academy.linktracker.bot.handler.dialog.UserSession;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Slf4j
public class ValkeyCscConfig {

    @Value("${app.redis.time-to-live}")
    private Duration ttl;

    @Bean
    public ChannelTopic cacheInvalidationTopic() {
        return new ChannelTopic("cache:invalidation:dialog");
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
    public Cache<String, UserSession> localCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new Expiry<String, UserSession>() {
                    @Override
                    public long expireAfterCreate(String key, UserSession value, long currentTime) {
                        return ttl.toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(
                            String key, UserSession value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(
                            String key, UserSession value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }
}
