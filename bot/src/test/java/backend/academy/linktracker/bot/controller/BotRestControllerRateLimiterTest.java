package backend.academy.linktracker.bot.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.bot.AbstractIntegrationTest;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class BotRestControllerRateLimiterTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelegramUpdateService telegramUpdateService;

    @DynamicPropertySource
    static void handleProperties(DynamicPropertyRegistry registry) {
        registry.add("resilience4j.ratelimiter.instances.api.limit-for-period", () -> 1);
        registry.add("resilience4j.ratelimiter.instances.api.limit-refresh-period", () -> "10s");
        registry.add("resilience4j.ratelimiter.instances.api.timeout-duration", () -> "0s");
    }

    @Test
    public void TC3_1_ExceedRateLimit_ShouldReturn429TooManyRequests() throws Exception {
        // given
        String jsonPayload = """
            {
                "id": 1,
                "url": "https://example.com",
                "description": "Обновление",
                "tgChatIds": [100, 101]
            }
            """;

        // when

        mockMvc.perform(post("/updates").contentType(MediaType.APPLICATION_JSON).content(jsonPayload))
                // then
                .andExpect(status().isOk());

        // when
        mockMvc.perform(post("/updates").contentType(MediaType.APPLICATION_JSON).content(jsonPayload))
                // then
                .andExpect(status().isTooManyRequests());
    }
}
