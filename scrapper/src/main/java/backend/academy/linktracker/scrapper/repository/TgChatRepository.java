package backend.academy.linktracker.scrapper.repository;

import java.util.Optional;
import java.util.Set;

public interface TgChatRepository {

    boolean save(Long chatId);

    boolean delete(Long id);

    Optional<Long> findByChatId(Long chatId);

    Set<Long> findAll();
}
