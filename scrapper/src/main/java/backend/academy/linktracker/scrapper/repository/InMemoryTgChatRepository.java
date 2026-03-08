package backend.academy.linktracker.scrapper.repository;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTgChatRepository implements TgChatRepository {

    private final Set<Long> chatIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean save(Long chatId) {
        return chatIds.add(chatId);
    }

    @Override
    public boolean delete(Long id) {
        return chatIds.remove(id);
    }

    @Override
    public Optional<Long> findByChatId(Long chatId) {
        if (chatIds.contains(chatId)) {
            return Optional.of(chatId);
        }
        return Optional.empty();
    }

    @Override
    public Set<Long> findAll() {
        return this.chatIds;
    }
}
