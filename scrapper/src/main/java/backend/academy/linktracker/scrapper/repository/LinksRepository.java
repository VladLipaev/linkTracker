package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.entity.Link;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface LinksRepository extends BaseRepository<Link, Long> {

    List<Long> findAllChatIdsByUrl(String url);

    Slice<Long> findAllChatIdsByUrl(String url, Pageable pageable);

    void updateLastUpdatedByUrl(String url, OffsetDateTime lastUpdateFromApi);

    Optional<Link> findByChatIdAndUrl(Long chatId, String url);

    Optional<Link> findByUrl(String url);

    // Найти ссылки, отсортированные по lastCheckedAt (сначала самые старые), с лимитом
    List<Link> findLinksToCheck(int batchSize);

    // Обновить время проверки (чтобы в следующий раз взять другие ссылки)
    void updateLastCheckedAt(List<Long> linkIds, OffsetDateTime checkedAt);
}
