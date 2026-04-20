package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.repository.orm.JpaLinksRepositoryInvoker;
import backend.academy.linktracker.scrapper.repository.raw.SqlLinksRepository;
import backend.academy.linktracker.scrapper.service.kafka.KafkaOutboxBatcher;
import backend.academy.linktracker.scrapper.service.kafka.KafkaOutboxWorker;
import backend.academy.linktracker.scrapper.service.kafka.KafkaTemplateConfiguration;
import backend.academy.linktracker.scrapper.service.kafka.KafkaTopicConfiguration;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestBeans.class)
@Transactional
public abstract class BaseLinksRepositoryTest {

    @Autowired
    private LinksRepository linksRepository;

    @Autowired
    private EntityManager entityManager;

    @Value("${app.db.access-type}")
    private String accessType;

    @MockitoBean
    private KafkaOutboxBatcher kafkaOutboxBatcher;

    @MockitoBean
    private KafkaOutboxWorker kafkaOutboxWorker;

    @MockitoBean
    private KafkaTopicConfiguration kafkaTopicConfiguration;

    @MockitoBean
    private KafkaTemplateConfiguration kafkaTemplateConfiguration;

    @MockitoBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @MockitoBean
    private KafkaAdmin kafkaAdmin;

    @Test
    @DisplayName("Проверка переключения access-type: используется правильная имплементация")
    void checkImplementation_shouldMatchAccessType() {
        // Проверяем, какой бин подтянулся в контекст

        if ("SQL".equalsIgnoreCase(accessType)) {
            assertThat(linksRepository).isInstanceOf(SqlLinksRepository.class);
        } else if ("ORM".equalsIgnoreCase(accessType)) {
            assertThat(linksRepository).isInstanceOf(JpaLinksRepositoryInvoker.class);
        }
    }

    @Test
    @DisplayName("Добавление ссылки: ссылка сохранена в БД")
    void saveLink_valid_shouldReturnLink() {
        // Given
        String url = "https://github.com/vlad/repo";
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Link link = new Link(url, now);

        // When
        Link saved = linksRepository.save(link);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUrl()).isEqualTo(url);
        // бд может отсекать наносекунды
        assertThat(saved.getLastUpdated()).isCloseTo(now, within(1, ChronoUnit.SECONDS));

        // Проверяем, что она реально есть в БД
        assertThat(linksRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Удаление ссылки: ссылка отсутствует в БД")
    void deleteLink_shouldRemoveFromDb() {
        // Given
        Link saved = linksRepository.save(new Link("https://google.com", OffsetDateTime.now()));
        Long id = saved.getId();

        // When
        linksRepository.deleteById(id);

        // Then
        Optional<Link> deleted = linksRepository.findById(id);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Добавление дублирующей ссылки: ожидается ошибка")
    void saveDuplicateLink_shouldThrowException() {
        // Given
        String url = "https://duplicate.com";
        linksRepository.save(new Link(url, OffsetDateTime.now()));

        // В зависимости от реализации может вылететь DataAccessException (Spring) или RawSqlException
        assertThrows(RuntimeException.class, () -> {
            linksRepository.save(new Link(url, OffsetDateTime.now()));
        });
    }

    @Test
    @DisplayName("Обновление времени ссылки: данные в БД изменяются")
    void updateLink_shouldChangeTimestamp() {
        // Given
        String url = "https://update-me.com";
        linksRepository.save(new Link(url, OffsetDateTime.now().minusDays(1)));
        OffsetDateTime newTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        entityManager.flush();
        entityManager.clear();
        // When
        linksRepository.updateLastUpdatedByUrl(url, newTime);

        // Then
        Link updated = linksRepository.findByUrl(url).orElseThrow();
        assertThat(updated.getLastUpdated()).isCloseTo(newTime, within(1, ChronoUnit.SECONDS));
    }
}
