package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.Link;
import java.time.OffsetDateTime;
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
public class JpaLinksRepositoryInvoker implements backend.academy.linktracker.scrapper.repository.LinksRepository {

    private final JpaLinksRepository jpaLinksRepository;

    @Override
    public List<Long> findAllChatIdsByUrl(String url) {
        return jpaLinksRepository.findAllChatIdsByUrl(url);
    }

    @Override
    public Slice<Long> findAllChatIdsByUrl(String url, Pageable pageable) {
        return jpaLinksRepository.findAllChatIdsByUrl(url, pageable);
    }

    @Override
    public void updateLastUpdatedByUrl(String url, OffsetDateTime lastUpdateFromApi) {
        jpaLinksRepository.updateLastUpdatedByUrl(url, lastUpdateFromApi);
    }

    @Override
    public Optional<Link> findByChatIdAndUrl(Long chatId, String url) {
        return jpaLinksRepository.findByChatIdAndUrl(chatId, url);
    }

    @Override
    public Optional<Link> findByUrl(String url) {
        return jpaLinksRepository.findByUrl(url);
    }

    @Override
    public List<Link> findLinksToCheck(int batchSize) {
        return jpaLinksRepository.findLinksToCheck(batchSize);
    }

    @Override
    public void updateLastCheckedAt(List<Long> linkIds, OffsetDateTime checkedAt) {
        jpaLinksRepository.updateLastCheckedAt(linkIds, checkedAt);
    }

    @Override
    public Slice<Link> findAll(Pageable pageable) {
        return jpaLinksRepository.findAll(pageable);
    }

    @Override
    public Link save(Link entity) {
        return jpaLinksRepository.save(entity);
    }

    @Override
    public Optional<Link> findById(Long id) {
        return jpaLinksRepository.findById(id);
    }

    @Override
    public List<Link> findAll() {
        return jpaLinksRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        jpaLinksRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaLinksRepository.existsById(id);
    }
}
