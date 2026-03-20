package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultTgChatService implements TgChatService {

    private final TgChatRepository tgChatRepository;

    @Override
    public void addTgChat(Long chatId) {
        if (tgChatRepository.existsById(chatId)) {
            throw new ChatAlreadyExistsException("Чат уже зарегистрирован");
        } else {
            tgChatRepository.save(new Chat(chatId));
        }
    }

    @Override
    public void deleteTgChat(Long chatId) {

        if (tgChatRepository.existsById(chatId)) {
            this.tgChatRepository.deleteById(chatId);
        } else {
            throw new ChatNotFoundException("Чат не существует");
        }
    }
}
