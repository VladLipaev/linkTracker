package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import backend.academy.linktracker.scrapper.repository.raw.SqlTgChatRepository;
import backend.academy.linktracker.scrapper.service.kafka.KafkaOutboxBatcher;
import backend.academy.linktracker.scrapper.service.kafka.KafkaOutboxWorker;
import backend.academy.linktracker.scrapper.service.kafka.KafkaTemplateConfiguration;
import backend.academy.linktracker.scrapper.service.kafka.KafkaTopicConfiguration;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class BaseSubscriptionRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @MockitoBean
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private TgChatRepository tgChatRepository;

    @Autowired
    private LinksRepository linksRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private KafkaOutboxBatcher kafkaOutboxBatcher;

    @MockitoBean
    private KafkaOutboxWorker kafkaOutboxWorker;

    @MockitoBean
    private KafkaTopicConfiguration kafkaTopicConfiguration;

    @MockitoBean
    private KafkaTemplateConfiguration kafkaTemplateConfiguration;

    @Value("${app.db.access-type}")
    private String accessType;

    private Long savedChatId;
    private Long savedLinkId;
    private Chat chat;
    private Link link;
    private final Pageable pageable = PageRequest.of(0, 10);

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
    @DisplayName("Изоляция тегов: разные чаты имеют разные теги для одной и той же ссылки")
    void tagsIsolation_multipleChatsSameLink_shouldNotMixTags() {
        Chat chatAlice = tgChatRepository.save(new Chat(111L));
        Chat chatBob = tgChatRepository.save(new Chat(222L));

        Link sharedLink = linksRepository.save(new Link("https://github.com/shared/repo", OffsetDateTime.now()));
        Long linkId = sharedLink.getId();

        Subscription subAlice = new Subscription(chatAlice.getId(), linkId);
        subAlice.setChat(chatAlice);
        subAlice.setLink(sharedLink);
        subAlice.setTags(List.of("work"));
        subscriptionRepository.save(subAlice);

        Subscription subBob = new Subscription(chatBob.getId(), linkId);
        subBob.setChat(chatBob);
        subBob.setLink(sharedLink);
        subBob.setTags(List.of("hobby"));
        subscriptionRepository.save(subBob);

        List<String> aliceTags = subscriptionRepository
                .findTagsByChatIdAndLinkId(chatAlice.getId(), linkId, pageable)
                .getContent();
        assertThat(aliceTags).containsExactly("work").doesNotContain("hobby");

        List<String> bobTags = subscriptionRepository
                .findTagsByChatIdAndLinkId(chatBob.getId(), linkId, pageable)
                .getContent();
        assertThat(bobTags).containsExactly("hobby").doesNotContain("work");

        List<Subscription> aliceSearch = subscriptionRepository
                .findSubscriptionsByChatIdAndTag(chatAlice.getId(), "hobby", pageable)
                .getContent();
        assertThat(aliceSearch).isEmpty();
    }

    @Test
    @DisplayName("Подписка с тегами: теги сохраняются и считываются")
    void saveWithTags_shouldPersistTags() {
        Subscription sub = new Subscription(savedChatId, savedLinkId);
        sub.setChat(chat);
        sub.setLink(link);
        sub.setTags(List.of("news", "social"));

        subscriptionRepository.save(sub);

        List<String> tags = subscriptionRepository
                .findTagsByChatIdAndLinkId(savedChatId, savedLinkId, pageable)
                .getContent();
        assertThat(tags).containsExactlyInAnyOrder("news", "social");
    }

    @Test
    @DisplayName("Поиск по тегу: возвращает только подходящие подписки")
    void findByTag_shouldFilterResults() {
        Subscription sub = new Subscription(savedChatId, savedLinkId);
        sub.setChat(chat);
        sub.setLink(link);
        sub.setTags(List.of("java"));
        subscriptionRepository.save(sub);

        List<Subscription> results = subscriptionRepository
                .findSubscriptionsByChatIdAndTag(savedChatId, "java", pageable)
                .getContent();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLink().getId()).isEqualTo(savedLinkId);

        assertThat(subscriptionRepository
                        .findSubscriptionsByChatIdAndTag(savedChatId, "python", pageable)
                        .getContent())
                .isEmpty();
    }

    @Test
    @DisplayName("Удалить Ссылку: подписки и теги должны удалиться")
    void deleteLink_shouldDeleteSubsAndTags() {
        Subscription sub = new Subscription(savedChatId, savedLinkId);
        sub.setTags(List.of("lalala"));
        sub.setLink(link);
        sub.setChat(chat);
        subscriptionRepository.save(sub);

        entityManager.flush();
        entityManager.clear();

        linksRepository.deleteById(savedLinkId);

        entityManager.flush();
        entityManager.clear();

        assertThat(linksRepository.findById(savedLinkId)).isEmpty();
        assertThat(subscriptionRepository.findById(new SubscriptionId(savedChatId, savedLinkId)))
                .isEmpty();
        assertThat(subscriptionRepository
                        .findTagsByChatIdAndLinkId(savedChatId, savedLinkId, pageable)
                        .getContent())
                .doesNotContain("lalala");
    }

    @Test
    @DisplayName("Проверка имплементации: должен использоваться правильный бин")
    void checkImplementation() {
        Object bean = applicationContext.getBean(TgChatRepository.class);
        if ("SQL".equalsIgnoreCase(accessType)) {
            assertThat(bean).isInstanceOf(SqlTgChatRepository.class);
        } else {
            assertThat(bean.getClass().getSimpleName()).contains("Jpa");
        }
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
