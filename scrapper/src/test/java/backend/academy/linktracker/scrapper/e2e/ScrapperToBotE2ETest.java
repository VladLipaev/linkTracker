package backend.academy.linktracker.scrapper.e2e;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.BotApplication;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.scrapper.ScrapperApplication;
import backend.academy.linktracker.scrapper.config.KafkaConfiguration;
import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.service.NotificationUpdateSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = {BotApplication.class, ScrapperApplication.class})
@Testcontainers
@Import({TestBeans.class, KafkaConfiguration.class, ScrapperToBotE2ETest.MockBotConfig.class})
@ActiveProfiles({"test-kafka-e2e"})
public class ScrapperToBotE2ETest {

    @Autowired
    NotificationUpdateSender notificationUpdateSender;

    @MockitoBean
    ScrapperClient scrapperClient;

    @Autowired
    TelegramBot telegrammBot;

    @TestConfiguration
    static class MockBotConfig {

        @Bean
        @Primary
        public TelegramBot telegrammBot() {
            TelegramBot mockBot = Mockito.mock(TelegramBot.class);
            BaseResponse dummyResponse = Mockito.mock(BaseResponse.class);
            Mockito.when(dummyResponse.isOk()).thenReturn(true);
            Mockito.when(mockBot.execute(Mockito.any())).thenReturn(dummyResponse);

            return mockBot;
        }
    }

    @Test
    void scrapperOutbox_kafka_botListener_callsTelegram() {
        notificationUpdateSender.sendUpdate(
                new LinkUpdate(1L, "https://github.com/VladLipaev/finance-tracker", "ok", List.of(42L)));
        verify(telegrammBot, timeout(30_000)).execute(any(SendMessage.class));
    }
}
