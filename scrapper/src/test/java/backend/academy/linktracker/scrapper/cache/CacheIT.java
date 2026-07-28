package backend.academy.linktracker.scrapper.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import backend.academy.linktracker.scrapper.service.ScrapperLinksService;
import com.github.benmanes.caffeine.cache.Cache;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public class CacheIT extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
                "app.kafka.schema-registry",
                () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
        registry.add("app.redis.time-to-live", () -> "2s");
    }

    @Autowired
    private ScrapperLinksService scrapperLinksService;

    @Autowired
    private TgChatRepository tgChatRepository;

    @Autowired
    private RedisTemplate<String, ListLinksResponse> redisTemplate;

    @Autowired
    private Cache<String, ListLinksResponse> localCache;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void shouldCacheDataInValkeyInExpectedFormat() {
        // given
        Long chatId = 100L;
        String expectedKey = "links:byTgChat::100::all";
        tgChatRepository.save(new Chat(chatId));
        scrapperLinksService.addLink(
                chatId, AddLinkRequest.builder().link(URI.create("https://github.com/VladLipaev/linkTracker")).tags(List.of()).build());

        // when
        ListLinksResponse response = scrapperLinksService.getLinks(chatId, null);

        // then
        Boolean hasKey = redisTemplate.hasKey(expectedKey);
        assertThat(hasKey).isTrue();
        ListLinksResponse cachedResponseValkey = redisTemplate.opsForValue().get(expectedKey);
        ListLinksResponse cachedResponseLocal = localCache.getIfPresent(expectedKey);
        assertThat(cachedResponseLocal).isNotNull();
        assertThat(cachedResponseValkey).isNotNull();
        assertThat(cachedResponseValkey.getSize()).isEqualTo(response.getSize());
        assertThat(cachedResponseLocal.getSize()).isEqualTo(response.getSize());
    }

    @Test
    void shouldInvalidateCacheOnAddLink() {
        // given
        Long chatId = 101L;
        String expectedKey = "links:byTgChat::101::all";
        tgChatRepository.save(new Chat(chatId));
        scrapperLinksService.addLink(
                chatId, AddLinkRequest.builder().link(URI.create("https://github.com/VladLipaev/finance-tracker")).tags(List.of()).build());
        // when
        scrapperLinksService.getLinks(chatId, null);
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
        assertThat(localCache.getIfPresent(expectedKey)).isNotNull();
        scrapperLinksService.addLink(
            chatId, AddLinkRequest.builder().link(URI.create("https://github.com/VladLipaev/linkTracker")).tags(List.of()).build());

        // then
        assertThat(redisTemplate.hasKey(expectedKey)).isFalse();
        assertThat(localCache.getIfPresent(expectedKey)).isNull();
    }

    @Test
    void shouldInvalidateCacheOnRemoveLink() {
        // given
        Long chatId = 103L;
        String expectedKey = "links:byTgChat::103::all";
        tgChatRepository.save(new Chat(chatId));
        scrapperLinksService.addLink(
            chatId, AddLinkRequest.builder().link(URI.create("https://github.com/VladLipaev/finance-tracker")).tags(List.of()).build());

        // when
        scrapperLinksService.getLinks(chatId, null);
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
        assertThat(localCache.getIfPresent(expectedKey)).isNotNull();
        scrapperLinksService.removeLink(chatId, RemoveLinkRequest.builder().link(URI.create("https://github.com/VladLipaev/finance-tracker")).build());

        // then
        assertThat(redisTemplate.hasKey(expectedKey)).isFalse();
        assertThat(localCache.getIfPresent(expectedKey)).isNull();
    }

    @Test
    void shouldExpireCacheAfterTtl() {
        // given
        Long chatId = 102L;
        String expectedKey = "links:byTgChat::102::all";
        tgChatRepository.save(new Chat(chatId));
        scrapperLinksService.addLink(
            chatId, AddLinkRequest.builder().link(URI.create("https://github.com/VladLipaev/Faceofgeneration")).tags(List.of()).build());

        // when
        scrapperLinksService.getLinks(chatId, null);
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
        assertThat(localCache.getIfPresent(expectedKey)).isNotNull();
        // then
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(redisTemplate.hasKey(expectedKey)).isFalse();
            assertThat(localCache.getIfPresent(expectedKey)).isNull();
        });
    }
}
