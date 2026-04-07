package backend.academy.linktracker.scrapper.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;

import backend.academy.linktracker.scrapper.entity.Chat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test-sql"})
public class SqlLinksRepositoryTest extends BaseLinksRepositoryTest {

    @Autowired
    private TgChatRepository tgChatRepository;

    @Test
    @DisplayName("Повторная регистрация ID: должна быть ошибка UNIQUE")
    void saveDuplicate_shouldThrowException() {
        Long chatId = 555L;
        tgChatRepository.save(new Chat(chatId));

        assertThrows(RawSqlException.class, () -> {
            tgChatRepository.save(new Chat(chatId));
        });
    }
}
