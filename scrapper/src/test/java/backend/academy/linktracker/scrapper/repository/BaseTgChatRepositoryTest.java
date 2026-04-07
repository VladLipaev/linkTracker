package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.config.TestBeans;
import backend.academy.linktracker.scrapper.entity.Chat;
// Предполагаемое имя ORM обертки
import backend.academy.linktracker.scrapper.repository.raw.SqlTgChatRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestBeans.class)
@Transactional
public abstract class BaseTgChatRepositoryTest {

    @Autowired
    private TgChatRepository tgChatRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${app.db.access-type}")
    private String accessType;

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
    @DisplayName("Сохранение чата: чат появляется в БД")
    void saveChat_shouldPersist() {
        Long chatId = 123456789L;
        Chat chat = new Chat(chatId);

        tgChatRepository.save(chat);

        Optional<Chat> found = tgChatRepository.findById(chatId);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(chatId);
    }

    @Test
    @DisplayName("Удаление чата: чат исчезает из БД")
    void deleteChat_shouldRemove() {
        Long chatId = 999L;
        tgChatRepository.save(new Chat(chatId));

        tgChatRepository.deleteById(chatId);

        assertThat(tgChatRepository.existsById(chatId)).isFalse();
    }
}
