package backend.academy.linktracker.scrapper.service;

public interface TgChatService {

    void addTgChat(Long chatId);

    void deleteTgChat(Long id);
}
