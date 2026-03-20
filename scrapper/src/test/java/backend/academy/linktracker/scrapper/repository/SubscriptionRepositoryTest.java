package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.TestBeans;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestBeans.class)
@ActiveProfiles("test")
@Transactional
public class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TgChatRepository tgChatRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LinksRepository linksRepository;

    private Long savedChatId;
    private Long savedLinkId;
    private Chat chat;
    private Link link;

    @BeforeEach
    void setUp() {
        // Для подписки нужны существующий чат и ссылка
        chat = tgChatRepository.save(new Chat(777L));
        link = linksRepository.save(new Link("https://t.me/news", OffsetDateTime.now()));

        savedChatId = chat.getId();
        savedLinkId = link.getId();
    }

    @Test
    @DisplayName("Создание подписки: связь сохраняется")
    void saveSubscription_shouldPersist() {
        Subscription sub = new Subscription(savedChatId, savedLinkId);
        sub.setChat(chat);
        sub.setLink(link);
        subscriptionRepository.save(sub);

        Optional<Subscription> found = subscriptionRepository.findById(new SubscriptionId(savedChatId, savedLinkId));
        assertThat(found).isPresent();
        assertThat(found.get().getSubscriptionId().getChatId()).isEqualTo(savedChatId);
    }

    @Test
    @DisplayName("Подписка с тегами: теги сохраняются и считываются")
    void saveWithTags_shouldPersistTags() {
        Subscription sub = new Subscription(savedChatId, savedLinkId);
        sub.setChat(chat);
        sub.setLink(link);
        sub.setTags(List.of("news", "social"));

        subscriptionRepository.save(sub);

        List<String> tags = subscriptionRepository.findTagsByChatIdAndLinkId(savedChatId, savedLinkId);
        assertThat(tags).containsExactlyInAnyOrder("news", "social");
    }

    @Test
    @DisplayName("Поиск по тегу: возвращает только подходящие подписки")
    void findByTag_shouldFilterResults() {
        // Создаем подписку с тегом
        Subscription sub = new Subscription(savedChatId, savedLinkId);
        sub.setChat(chat);
        sub.setLink(link);
        sub.setTags(List.of("java"));
        subscriptionRepository.save(sub);

        List<Subscription> results = subscriptionRepository.findSubscriptionsByChatIdAndTag(savedChatId, "java");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLink().getId()).isEqualTo(savedLinkId);

        assertThat(subscriptionRepository.findSubscriptionsByChatIdAndTag(savedChatId, "python"))
                .isEmpty();
    }

    @Test
    @DisplayName("Удаление подписки: связь исчезает, но чат и ссылка остаются")
    void deleteSubscription_shouldRemoveRelationOnly() {
        Subscription sub = new Subscription(savedChatId, savedLinkId);
        sub.setChat(chat);
        sub.setLink(link);
        subscriptionRepository.save(sub);

        subscriptionRepository.deleteBySubscriptionId(new SubscriptionId(savedChatId, savedLinkId));

        // Связи нет
        assertThat(subscriptionRepository.existsById(new SubscriptionId(savedChatId, savedLinkId)))
                .isFalse();
        // Сущности на месте
        assertThat(tgChatRepository.existsById(savedChatId)).isTrue();
        assertThat(linksRepository.existsById(savedLinkId)).isTrue();
    }

    @Test
    @DisplayName("Проверка existsByLinkId: корректно определяет наличие подписчиков")
    void existsByLinkId_shouldWorkCorrectly() {

        assertThat(subscriptionRepository.existsByLinkId(savedLinkId)).isFalse();
        Subscription sub = new Subscription(savedChatId, savedLinkId);
        sub.setChat(chat);
        sub.setLink(link);
        subscriptionRepository.save(sub);

        assertThat(subscriptionRepository.existsByLinkId(savedLinkId)).isTrue();
    }
}
