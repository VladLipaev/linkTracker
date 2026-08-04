package backend.academy.linktracker.scrapper.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

public class ScrapperRestControllerRateLimiterTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("resilience4j.ratelimiter.configs.api.limit-for-period", () -> 1);
        registry.add("resilience4j.ratelimiter.configs.api.limit-refresh-period", () -> "10s");
        registry.add("resilience4j.ratelimiter.configs.api.timeout-duration", () -> "0s");
        registry.add("app.redis.time-to-live", () -> "2s");
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
