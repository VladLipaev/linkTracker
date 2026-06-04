package backend.academy.linktracker.ai.controller.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.config.FilteringProperties;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RawLinkUpdateValidatorTest {

    @Mock
    private FilteringProperties filteringProperties;

    private RawLinkUpdateValidator validator;

    private static final String VALID_AUTHOR = "JohnDoe";
    private static final String EXCLUDED_AUTHOR = "self";
    private static final int MIN_LENGTH = 250;

    @BeforeEach
    void setUp() {
        when(filteringProperties.stopWords()).thenReturn(List.of("spam", "ads", "promo"));
        when(filteringProperties.excludedAuthors()).thenReturn(List.of(EXCLUDED_AUTHOR));
        when(filteringProperties.minLength()).thenReturn(MIN_LENGTH);
        validator = new RawLinkUpdateValidator(filteringProperties);
        validator.init();
    }

    @Test
    @DisplayName("TC-2.1: Фильтрация по стоп-словам")
    void shouldRejectWhenDescriptionContainsStopWord() {
        RawLinkUpdateAvro avro = new RawLinkUpdateAvro();
        avro.setAuthor(VALID_AUTHOR);
        avro.setDescription("Check out this spammy offer!");

        assertThat(validator.validate(avro)).isFalse();
    }

    @Test
    @DisplayName("TC-2.2: Фильтрация по исключённому автору")
    void shouldRejectWhenAuthorIsExcluded() {
        RawLinkUpdateAvro avro = new RawLinkUpdateAvro();
        avro.setAuthor(EXCLUDED_AUTHOR);
        avro.setDescription("A".repeat(MIN_LENGTH + 1));

        assertThat(validator.validate(avro)).isFalse();
    }

    @Test
    @DisplayName("TC-2.3: Фильтрация по минимальной длине")
    void shouldRejectWhenDescriptionIsTooShort() {
        RawLinkUpdateAvro avro = new RawLinkUpdateAvro();
        avro.setAuthor(VALID_AUTHOR);
        avro.setDescription("Short");

        assertThat(validator.validate(avro)).isFalse();
    }

    @Test
    @DisplayName("TC-2.4: Валидное обновление проходит фильтрацию")
    void shouldAcceptValidUpdate() {
        RawLinkUpdateAvro avro = new RawLinkUpdateAvro();
        avro.setAuthor(VALID_AUTHOR);
        avro.setDescription("A".repeat(MIN_LENGTH + 1));

        assertThat(validator.validate(avro)).isTrue();
    }

    @Test
    @DisplayName("Стоп-слова проверяются без учёта регистра")
    void shouldRejectStopWordCaseInsensitive() {
        RawLinkUpdateAvro avro = new RawLinkUpdateAvro();
        avro.setAuthor(VALID_AUTHOR);
        avro.setDescription("This is SpAm content");

        assertThat(validator.validate(avro)).isFalse();
    }
}
