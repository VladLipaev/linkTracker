package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.access-type", havingValue = "ORM")
public class JpaTgChatRepositoryInvoker implements TgChatRepository {

    private final JpaTgChatRepository jpaTgChatRepository;

    @Override
    public Chat save(Chat chat) {
        return jpaTgChatRepository.save(chat);
    }

    @Override
    public Optional<Chat> findById(Long id) {
        return jpaTgChatRepository.findById(id);
    }

    @Override
    public List<Chat> findAll() {
        return jpaTgChatRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        jpaTgChatRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long chatId) {
        return jpaTgChatRepository.existsById(chatId);
    }
}
