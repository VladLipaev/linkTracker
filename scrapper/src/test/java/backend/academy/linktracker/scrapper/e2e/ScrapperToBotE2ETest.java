package backend.academy.linktracker.scrapper.e2e;

import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.KAFKA_CONTAINER;
import static backend.academy.linktracker.scrapper.config.KafkaConfiguration.SCHEMA_REGISTRY;
import static backend.academy.linktracker.scrapper.config.TestBeans.POSTGRES;
import static backend.academy.linktracker.scrapper.config.TestBeans.VALKEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.BotApplication;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.scrapper.ScrapperApplication;
import backend.academy.linktracker.scrapper.client.bot.RestClientTelegramBotClient;
import backend.academy.linktracker.scrapper.config.KafkaConfiguration;
import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.service.SyncNotificationUpdateSender;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = {BotApplication.class, ScrapperApplication.class})
@Testcontainers
@Import({TestBeans.class, KafkaConfiguration.class, ScrapperToBotE2ETest.MockBotConfig.class})
@ActiveProfiles({"test-kafka-e2e"})
public class ScrapperToBotE2ETest {

    static {
        POSTGRES.start();
        KAFKA_CONTAINER.start();
        SCHEMA_REGISTRY.start();
        VALKEY.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add(
                "app.kafka.schema-registry",
                () -> "http://" + SCHEMA_REGISTRY.getHost() + ":" + SCHEMA_REGISTRY.getFirstMappedPort());
    }

    @Autowired
    private SyncNotificationUpdateSender notificationUpdateSender;

    @MockitoBean
    private ScrapperClient scrapperClient;

    @Autowired
    private TelegramBot telegrammBot;

    @MockitoSpyBean
    private RestClientTelegramBotClient restClientTelegramBotClient;

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

        @Bean
        @Primary
        public ProducerFactory<String, Object> primaryProducerFactory(
                @Qualifier("scrapperDlqProducerFactory") ProducerFactory<String, Object> producerFactory) {
            return producerFactory;
        }

        @Bean
        @Primary
        public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }
    }

    @Test
    void scrapperOutbox_kafka_botListener_callsTelegram() {
        doThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
                .when(restClientTelegramBotClient)
                .sendUpdate(any());
        notificationUpdateSender.sendUpdate(
                new LinkUpdate(1L, "https://github.com/VladLipaev/finance-tracker", "ok", List.of(42L)));
        verify(telegrammBot, timeout(30_000)).execute(any(SendMessage.class));
    }
}
