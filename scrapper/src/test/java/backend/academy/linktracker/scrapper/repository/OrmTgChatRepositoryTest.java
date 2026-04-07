package backend.academy.linktracker.scrapper.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.service.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.service.TgChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test-orm"})
public class OrmTgChatRepositoryTest extends BaseTgChatRepositoryTest {

    @Autowired
    private TgChatRepository tgChatRepository;

    @Autowired
    private TgChatService tgChatService;

    @Test
    @DisplayName("Повторная регистрация ID: должна быть ошибка UNIQUE")
    // jpa реализация не выкидывает исключение а обрабатывает его сама
    void saveDuplicate_shouldThrowException() {
        Long chatId = 555L;
        tgChatRepository.save(new Chat(chatId));

        assertThrows(ChatAlreadyExistsException.class, () -> {
            tgChatService.addTgChat(chatId);
        });
    }
}
