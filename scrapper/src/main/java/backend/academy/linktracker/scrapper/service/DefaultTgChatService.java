package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.ChatAndListLinksResponse;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaTgChatRepositoryInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;

@Service
@RequiredArgsConstructor
public class DefaultTgChatService implements TgChatService {

    private final JpaTgChatRepositoryInvoker tgChatRepository;

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
        return ChatAndListLinksResponse.builder()
            .links(chat.getSubscriptions()
                .stream()
                .map(sub -> LinkResponse
                    .builder()
                    .id(sub.getLink().getId())
                    .url(URI.create(sub.getLink().getUrl()))
                    .tags(sub.getTags())
                    .lastCheckedAt(sub.getLink().getLastCheckedAt()).build())
                    .toList()
                )
            .chatId(chat.getId())
            .build();
    }
}
