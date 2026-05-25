package backend.academy.linktracker.scrapper.controller;

import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.KAFKA_CONTAINER;
import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.SCHEMA_REGISTRY;
import static backend.academy.linktracker.scrapper.config.TestBeans.POSTGRES;
import static backend.academy.linktracker.scrapper.config.TestBeans.VALKEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.config.KafkaConfiguration;
import backend.academy.linktracker.scrapper.config.TestBeans;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestBeans.class, KafkaConfiguration.class})
public class ScrapperRestControllerRateLimiterTest {
    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
        VALKEY.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("resilience4j.ratelimiter.configs.api.limit-for-period", () -> 1);
        registry.add("resilience4j.ratelimiter.configs.api.limit-refresh-period", () -> "10s");
        registry.add("resilience4j.ratelimiter.configs.api.timeout-duration", () -> "0s");
        registry.add("app.redis.time-to-live", () -> "2s");
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
                "app.kafka.schema-registry",
                () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
    }

    @Test
    public void TC3_1_ExceedRateLimit_ShouldReturn429TooManyRequests() throws Exception {
        // given
        String endpoint = "/tg-chat/100";

        // when

        mockMvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON))
                // then
                .andExpect(status().isOk());

        // when
        mockMvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON))
                // then
                .andExpect(status().isTooManyRequests());
    }
}
