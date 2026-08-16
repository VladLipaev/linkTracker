package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.repository.orm.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.utils.SubscriptionSpecifications;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.configuration.Orthography;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.spring.api.DBRider;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@DBRider
@DBUnit(schema = "public", caseInsensitiveStrategy = Orthography.LOWERCASE) // Явно указываем схему
@DataSet(value = "datasets/chats_links_subscriptions.yml", cleanBefore = true, cleanAfter = true)
class JpaSubscriptionRepositorySpecificationTest extends AbstractIntegrationTest {

    @Autowired
    private JpaSubscriptionRepository repository;

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
