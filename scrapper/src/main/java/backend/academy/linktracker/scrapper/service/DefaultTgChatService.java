package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultTgChatService implements TgChatService {

    private final TgChatRepository tgChatRepository;

    @Override
    public void addTgChat(Long chatId) {
        boolean isNew = tgChatRepository.save(chatId);
        if (!isNew) {
            throw new ChatAlreadyExistsException("Чат уже зарегистрирован");
        }
    }

    @Override
    public void deleteTgChat(Long chatId) {
        boolean isGone = this.tgChatRepository.delete(chatId);
        if (!isGone) {
            throw new ChatNotFoundException("Чат не существует");
        }
    }
}
