package backend.academy.linktracker.scrapper.cache;

import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.KAFKA_CONTAINER;
import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.SCHEMA_REGISTRY;
import static backend.academy.linktracker.scrapper.config.TestBeans.POSTGRES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import backend.academy.linktracker.scrapper.config.KafkaConfiguration;
import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import backend.academy.linktracker.scrapper.service.ScrapperLinksService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Testcontainers
@Import({TestBeans.class, KafkaConfiguration.class, CacheIT.TestRedisConfig.class})
public class CacheIT {

    @Container
    public static GenericContainer<?> valkey = new GenericContainer<>("valkey/valkey:latest")
        .withExposedPorts(6379);

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
    }

    @TestConfiguration
    static class TestRedisConfig {
        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory(valkey.getHost(), valkey.getFirstMappedPort());
        }
    }

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

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void shouldCacheDataInValkeyInExpectedFormat() {
        //given
        Long chatId = 100L;
        String expectedKey = "links:byTgChat::100::all";
        tgChatRepository.save(new Chat(chatId));
        scrapperLinksService.addLink(chatId, new AddLinkRequest("https://github.com/VladLipaev/linkTracker", List.of()));


        //when
        ListLinksResponse response = scrapperLinksService.getLinks(chatId, null);

        //then
        Boolean hasKey = redisTemplate.hasKey(expectedKey);
        assertThat(hasKey).isTrue();
        ListLinksResponse cachedResponse = redisTemplate.opsForValue().get(expectedKey);
        assertThat(cachedResponse).isNotNull();
        assertThat(cachedResponse.size()).isEqualTo(response.size());
    }

    @Test
    void shouldInvalidateCacheOnAddLink() {
        //given
        Long chatId = 101L;
        String expectedKey = "links:byTgChat::101::all";
        tgChatRepository.save(new Chat(chatId));
        scrapperLinksService.addLink(chatId, new AddLinkRequest("https://github.com/VladLipaev/finance-tracker", List.of()));

        //when
        scrapperLinksService.getLinks(chatId, null);
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
        scrapperLinksService.addLink(chatId, new AddLinkRequest("https://github.com/VladLipaev/linkTracker", List.of()));

        //then
        assertThat(redisTemplate.hasKey(expectedKey)).isFalse();
    }

    @Test
    void shouldExpireCacheAfterTtl() {
        //given
        Long chatId = 102L;
        String expectedKey = "links:byTgChat::102::all";
        tgChatRepository.save(new Chat(chatId));
        scrapperLinksService.addLink(chatId, new AddLinkRequest("https://github.com/VladLipaev/Faceofgeneration", List.of()));

        //when
        scrapperLinksService.getLinks(chatId, null);
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();

        //then
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(redisTemplate.hasKey(expectedKey)).isFalse()
        );
    }
}
