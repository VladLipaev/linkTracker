package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.repository.orm.JpaLinksRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaTgChatRepository;
import backend.academy.linktracker.scrapper.repository.utils.SubscriptionSpecifications;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class JpaSubscriptionRepositorySpecificationTest extends AbstractIntegrationTest {

    @Autowired
    private JpaSubscriptionRepository repository;

    @Autowired
    private JpaTgChatRepository tgChatRepository;

    @Autowired
    private JpaLinksRepository linksRepository;


    private Chat chat1;
    private Chat chat2;
    private Link link1;
    private Link link2;
    private Link link3;

    @BeforeEach
    @Transactional
    void setUp() {
        // Создаём чаты
        chat1 = new Chat(1L);
        chat2 = new Chat(2L);
        tgChatRepository.save(chat1);
        tgChatRepository.save(chat2);
        // Создаём ссылки
        link1 = Link.builder()
            .url("https://github.com/spring-projects/spring-boot")
            .lastUpdated(OffsetDateTime.now())
            .build();
        link1.setLastCheckedAt(OffsetDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC));
       linksRepository.save(link1);

        link2 = Link.builder()
            .url("https://github.com/spring-projects/spring-framework")
            .lastUpdated(OffsetDateTime.now())
            .build();
        link2.setLastCheckedAt(OffsetDateTime.of(2025, 2, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        linksRepository.save(link2);

        link3 = Link.builder()
            .url("https://github.com/apache/kafka")
            .lastUpdated(OffsetDateTime.now())
            .build();
        link3.setLastCheckedAt(OffsetDateTime.of(2025, 3, 10, 15, 0, 0, 0, ZoneOffset.UTC));
        linksRepository.save(link3);

        // Создаём подписки
        Subscription sub1 = new Subscription(chat1.getId(), link1.getId());
        sub1.setChat(chat1);
        sub1.setLink(link1);
        sub1.setTags(List.of("java", "spring"));
        repository.save(sub1);

        Subscription sub2 = new Subscription(chat1.getId(), link2.getId());
        sub2.setChat(chat1);
        sub2.setLink(link2);
        sub2.setTags(List.of("java", "framework"));
        repository.save(sub2);

        Subscription sub3 = new Subscription(chat2.getId(), link3.getId());
        sub3.setChat(chat2);
        sub3.setLink(link3);
        sub3.setTags(List.of("kafka"));
        repository.save(sub3);
    }

    @Test
    void shouldFilterByTag() {
        Specification<Subscription> spec = SubscriptionSpecifications.hasTag("java");
        Pageable pageable = PageRequest.of(0, 10);

        Page<Subscription> result = repository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(sub -> sub.getLink().getUrl())
            .containsExactlyInAnyOrder(
                "https://github.com/spring-projects/spring-boot",
                "https://github.com/spring-projects/spring-framework"
            );
    }

    @Test
    void shouldFilterByUrlContains() {
        Specification<Subscription> spec = SubscriptionSpecifications.urlContains("spring");
        Pageable pageable = PageRequest.of(0, 10);

        Page<Subscription> result = repository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(sub -> sub.getLink().getUrl())
            .containsExactlyInAnyOrder(
                "https://github.com/spring-projects/spring-boot",
                "https://github.com/spring-projects/spring-framework"
            );
    }

    @Test
    void shouldFilterByChatId() {
        Specification<Subscription> spec = SubscriptionSpecifications.chatIdEquals(1L);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Subscription> result = repository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(sub -> sub.getChat().getId())
            .containsOnly(1L);
    }

    @Test
    void shouldFilterByUpdatedAfter() {
        Instant date = OffsetDateTime.of(2025, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Specification<Subscription> spec = SubscriptionSpecifications.updatedAfter(date);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Subscription> result = repository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(sub -> sub.getLink().getLastCheckedAt().toInstant())
            .allMatch(instant -> instant.isAfter(date) || instant.equals(date));
    }

    @Test
    void shouldFilterByUpdatedBefore() {
        Instant date = OffsetDateTime.of(2025, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Specification<Subscription> spec = SubscriptionSpecifications.updatedBefore(date);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Subscription> result = repository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(sub -> sub.getLink().getLastCheckedAt().toInstant())
            .allMatch(instant -> instant.isBefore(date) || instant.equals(date));
    }

    @Test
    void shouldCombineFilters() {
        // Явное приведение типа для разрешения перегрузки
        Specification<Subscription> spec = Specification
            .where((Specification<Subscription>) SubscriptionSpecifications.hasTag("java"))
            .and(SubscriptionSpecifications.urlContains("boot"));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Subscription> result = repository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLink().getUrl())
            .isEqualTo("https://github.com/spring-projects/spring-boot");
    }
    @Test
    void shouldSupportPagination() {
        Specification<Subscription> spec = null;
        Pageable pageable = PageRequest.of(0, 2);

        Page<Subscription> result = repository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void shouldSupportSorting() {
        Specification<Subscription> spec = null;
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "link.url"));

        Page<Subscription> result = repository.findAll(spec, pageable);

        List<String> urls = result.getContent().stream()
            .map(sub -> sub.getLink().getUrl())
            .toList();
        assertThat(urls).isSorted();
    }

    @Test
    void shouldFilterAndSortAndPaginate() {
        Specification<Subscription> spec = SubscriptionSpecifications.hasTag("java");
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "link.url"));

        Page<Subscription> result = repository.findAll(spec, pageable);

        assertThat(result.getContent()).hasSize(2);
        List<String> urls = result.getContent().stream()
            .map(sub -> sub.getLink().getUrl())
            .toList();
        assertThat(urls).containsExactly(
            "https://github.com/spring-projects/spring-framework",
            "https://github.com/spring-projects/spring-boot"
        );
    }
}
