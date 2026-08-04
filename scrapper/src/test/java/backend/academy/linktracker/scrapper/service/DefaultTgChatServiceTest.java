package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTgChatServiceTest {

    @Mock
    private TgChatRepository tgChatRepository;

    @InjectMocks
    private DefaultTgChatService tgChatService;

    private final Long chatId = 12345L;

    @Test
    @DisplayName("addTgChat — Успешное сохранение нового чата")
    void addTgChat_Success() {
        when(tgChatRepository.existsById(chatId)).thenReturn(false);

        tgChatService.addTgChat(chatId);

        verify(tgChatRepository).save(any(Chat.class));
    }

    @Test
    @DisplayName("addTgChat — Исключение, если чат уже существует")
    void addTgChat_AlreadyExists() {
        when(tgChatRepository.existsById(chatId)).thenReturn(true);

        assertThatThrownBy(() -> tgChatService.addTgChat(chatId))
            .isInstanceOf(ChatAlreadyExistsException.class)
            .hasMessage("Чат уже зарегистрирован");

        verify(tgChatRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteTgChat — Успешное удаление чата")
    void deleteTgChat_Success() {
        when(tgChatRepository.existsById(chatId)).thenReturn(true);

        tgChatService.deleteTgChat(chatId);

        verify(tgChatRepository).deleteById(chatId);
    }

    @Test
    @DisplayName("deleteTgChat — Исключение, если чат не найден")
    void deleteTgChat_NotFound() {
        when(tgChatRepository.existsById(chatId)).thenReturn(false);

        assertThatThrownBy(() -> tgChatService.deleteTgChat(chatId))
            .isInstanceOf(ChatNotFoundException.class)
            .hasMessage("Чат не существует");

        verify(tgChatRepository, never()).deleteById(any());
    }
}
