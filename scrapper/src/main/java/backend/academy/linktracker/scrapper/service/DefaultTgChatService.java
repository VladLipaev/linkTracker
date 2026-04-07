package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultTgChatService implements TgChatService {

    private final TgChatRepository tgChatRepository;

    @Override
    @Transactional
    public void addTgChat(Long chatId) {
        if (tgChatRepository.existsById(chatId)) {
            throw new ChatAlreadyExistsException("Чат уже зарегистрирован");
        } else {
            tgChatRepository.save(new Chat(chatId));
        }
    }

    @Override
    @Transactional
    public void deleteTgChat(Long chatId) {

        if (tgChatRepository.existsById(chatId)) {
            this.tgChatRepository.deleteById(chatId);
        } else {
            throw new ChatNotFoundException("Чат не существует");
        }
    }
}
