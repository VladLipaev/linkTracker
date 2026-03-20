package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.access-type", havingValue = "ORM")
public class JpaSubscriptionRepositoryInvoker
        implements backend.academy.linktracker.scrapper.repository.SubscriptionRepository {

    private final JpaSubscriptionRepository jpaRepository;

    @Override
    public Slice<Subscription> findSubscriptionsByChatIdAndTag(Long chatId, String tag, Pageable pageable) {
        return jpaRepository.findSubscriptionsByChatIdAndTag(chatId, tag, pageable);
    }

    @Override
    public List<Subscription> findSubscriptionsByChatIdAndTag(Long chatId, String tag) {
        return jpaRepository.findSubscriptionsByChatIdAndTag(chatId, tag);
    }

    @Override
    public void deleteBySubscriptionId(SubscriptionId id) {
        jpaRepository.deleteBySubscriptionId(id);
    }

    @Override
    public boolean existsByLinkId(Long linkId) {
        return jpaRepository.existsByLinkId(linkId);
    }

    @Override
    public List<String> findTagsByChatIdAndLinkId(Long chatId, Long linkId) {
        return jpaRepository.findTagsByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public Slice<String> findTagsByChatIdAndLinkId(Long chatId, Long linkId, Pageable pageable) {
        return jpaRepository.findTagsByChatIdAndLinkId(chatId, linkId, pageable);
    }

    @Override
    public Subscription save(Subscription entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Subscription> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(SubscriptionId id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(SubscriptionId id) {
        return jpaRepository.existsById(id);
    }
}
