package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.ChatAndListLinksResponse;
import org.jspecify.annotations.Nullable;

public interface TgChatService {

    void addTgChat(Long chatId);

    void deleteTgChat(Long id);

    ChatAndListLinksResponse getTgChatAndListLinks(Long id);
}
