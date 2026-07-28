package backend.academy.linktracker.scrapper.schedule;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import backend.academy.linktracker.scrapper.service.NotificationUpdateSender;
import backend.academy.linktracker.scrapper.service.SyncNotificationUpdateSender;
import backend.academy.linktracker.scrapper.service.kafka.KafkaNotificationUpdateSender;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@WireMockTest(httpPort = 54321)
public abstract class BaseLinkUpdaterSchedulerIT {

    @Autowired
    private LinkUpdaterScheduler scheduler;

    @Autowired
    private LinksRepository linksRepository;

    @Autowired
    private TgChatRepository tgChatRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private NotificationUpdateSender notificationUpdateSender; // Мокаем бота, чтобы проверять, что ему отправляется

    @Value("${app.communication.client.mode}")
    private String clientMode;

    private Chat testChat;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.github.base-url", () -> "http://localhost:54321");
        registry.add("app.stackoverflow.base-url", () -> "http://localhost:54321");
        registry.add("app.github.token", () -> "dummy-token");
        registry.add("app.stackoverflow.key", () -> "dummy-key");
    }

    @BeforeEach
    void setUp() {
        // Очищаем БД перед каждым тестом
        subscriptionRepository.findAll().forEach(s -> subscriptionRepository.deleteById(s.getSubscriptionId()));
        linksRepository.findAll().forEach(l -> linksRepository.deleteById(l.getId()));
        tgChatRepository.findAll().forEach(c -> tgChatRepository.deleteById(c.getId()));

        testChat = tgChatRepository.save(new Chat(12345L));
    }

    @Test
    void shouldUseClientModeFromConfig() {
        if (clientMode.equals("kafka")) {
            assertThat(notificationUpdateSender instanceof KafkaNotificationUpdateSender);
        } else if (clientMode.equals("rest") || clientMode.equals("grpc")) {
            assertThat(notificationUpdateSender instanceof SyncNotificationUpdateSender);
        } else {
            assertThat(notificationUpdateSender instanceof KafkaNotificationUpdateSender);
        }
    }

    @Test
    @DisplayName("Mock GitHub API возвращает новый Issue -> Scrapper формирует сообщение")
    void shouldFormMessageWithTitleAuthorPreview_ForNewGithubIssue() {
        // Arrange
        String url = "https://github.com/test-owner/test-repo";
        OffsetDateTime oldDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
        createSubscription(url, oldDate);

        // Имитируем ответ GitHub с новым Issue (дата создания новее oldDate)
        stubFor(get(urlPathMatching("/repos/test-owner/test-repo/issues")).willReturn(okJson("""
[
                  {
                    "title": "Упал прод",
                    "user": {"login": "crazy_hacker"},
                    "created_at": "2030-04-07T10:00:00Z",
                    "updated_at": "2030-04-07T10:00:00Z",
                    "body": "Все сломалось, чините быстрее"
                  }
                ]
                """)));

        // Act
        scheduler.update();

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(notificationUpdateSender).sendUpdate(captor.capture());

        String message = captor.getValue().description();
        assertThat(message)
                .contains("Упал прод")
                .contains("crazy_hacker")
                .contains("Все сломалось, чините быстрее")
                .contains("Новый Issue");
    }

    @Test
    @DisplayName("Текст превью длиннее 200 символов -> обрезается")
    void shouldTruncatePreviewTo200Characters() {
        String url = "https://github.com/test-owner/test-repo";
        createSubscription(url, OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        String longBody = "A".repeat(300); // 300 символов

        stubFor(get(urlPathMatching("/repos/test-owner/test-repo/issues")).willReturn(okJson("""
                  [
                  {
                    "title": "Длинный баг",
                    "user": {"login": "user1"},
                    "created_at": "2030-04-07T10:00:00Z",
                    "updated_at": "2030-04-07T10:00:00Z",
                    "body": "%s"
                  }
                ]
                """.formatted(longBody))));

        scheduler.update();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(notificationUpdateSender).sendUpdate(captor.capture());

        String preview = captor.getValue().description();
        String expectedPreview = "A".repeat(200) + "...";

        assertThat(preview).contains(expectedPreview);
        assertThat(preview).doesNotContain("A".repeat(201)); // Убеждаемся, что нет 201-й буквы
    }

    @Test
    @DisplayName("Mock StackOverflow API возвращает новый ответ -> формируется сообщение")
    void shouldFormMessage_ForNewStackOverflowAnswer() {
        String url = "https://stackoverflow.com/questions/123456";
        createSubscription(url, OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        // Мокаем получение самого вопроса
        stubFor(get(urlPathMatching("/questions/123456"))
                .willReturn(okJson(
                        "{\"items\":[{\"last_activity_date\": 2000000000, \"title\": \"Как выйти из VIM?\"}]}")));

        // Мокаем ответы
        stubFor(get(urlPathMatching("/questions/123456/answers")).willReturn(okJson("""
                {
                  "items":[
                    {
                      "creation_date": 2000000000,
                      "body_markdown": "Просто нажми :q!",
                      "owner": {"display_name": "senior_dev"}
                    }
                  ]
                }
                """)));

        stubFor(get(urlPathMatching("/questions/123456/comments")).willReturn(okJson("{\"items\":[]}")));

        scheduler.update();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(notificationUpdateSender).sendUpdate(captor.capture());

        String message = captor.getValue().description();
        assertThat(message)
                .contains("Как выйти из VIM?")
                .contains("Новый ответ")
                .contains("senior_dev")
                .contains("Просто нажми :q!");
    }

    @Test
    @DisplayName("Пакетная обработка: изоляция ошибок. Упала одна ссылка -> остальные обработаны")
    void batchProcessing_ErrorsAreIsolated_RestProcessed() {
        // Создаем 3 ссылки
        createSubscription(
                "https://github.com/user/ok-1",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
        createSubscription(
                "https://github.com/user/fail",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
        createSubscription(
                "https://github.com/user/ok-2",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));

        // Первая ссылка ОК
        stubFor(
                get(urlPathMatching("/repos/user/ok-1/issues"))
                        .willReturn(
                                okJson(
                                        "[{\"title\": \"OK 1\", \"user\": {\"login\": \"a\"}, \"created_at\": \"2030-04-07T10:00:00Z\", \"updated_at\": \"2030-04-07T10:00:00Z\"}]")));

        // Вторая ссылка падает с 500 ошибкой
        stubFor(get(urlPathMatching("/repos/user/fail/issues")).willReturn(serverError()));

        // Третья ссылка ОК
        stubFor(
                get(urlPathMatching("/repos/user/ok-2/issues"))
                        .willReturn(
                                okJson(
                                        "[{\"title\": \"OK 2\", \"user\": {\"login\": \"b\"}, \"created_at\": \"2030-04-07T10:00:00Z\", \"updated_at\": \"2030-04-07T10:00:00Z\"}]")));

        // Act
        scheduler.update();

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        // Проверяем, что бот получил ровно 3 сообщения (2 успешных и 1 ошибочный)
        verify(notificationUpdateSender, times(3)).sendUpdate(captor.capture());

        List<String> messages =
                captor.getAllValues().stream().map(LinkUpdate::description).toList();

        assertThat(messages).anyMatch(m -> m.contains("OK 1"));
        assertThat(messages).anyMatch(m -> m.contains("OK 2"));
        // Проверяем, что бот обработал ситуацию недоступности (сообщение об ошибке)
        assertThat(messages).anyMatch(m -> m.contains("Временная ошибка при обращении к ресурсу:"));
    }

    private void createSubscription(String url, OffsetDateTime lastUpdated) {
        Link link = new Link();
        link.setUrl(url);
        link.setLastUpdated(lastUpdated);
        link.setLastCheckedAt(lastUpdated); // Чтобы попала в батч
        link = linksRepository.save(link);

        Subscription sub = new Subscription(testChat.getId(), link.getId());
        sub.setChat(testChat);
        sub.setLink(link);
        subscriptionRepository.save(sub);
    }
}
