package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.ChatAndListLinksResponse;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaTgChatRepositoryInvoker;
import backend.academy.linktracker.scrapper.service.mapper.ChatAndListLinksResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultTgChatService implements TgChatService {

    private final JpaTgChatRepositoryInvoker tgChatRepository;
    private final ChatAndListLinksResponseMapper chatAndListLinksResponseMapper;

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

    @Override
    @Transactional(readOnly = true)
    public ChatAndListLinksResponse getTgChatAndListLinks(Long id) {
        Chat chat = tgChatRepository.getChatAndLinks(id).orElseThrow(() -> new ChatNotFoundException("Чат не существует"));
        return chatAndListLinksResponseMapper.toChatAndListLinksResponse(chat);
    }
}
