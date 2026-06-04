package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.Link;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLinksRepository extends JpaRepository<Link, Long> {

    @Query("SELECT c.id FROM Chat c JOIN c.subscriptions s JOIN s.link l WHERE l.url = :url")
    List<Long> findAllChatIdsByUrl(@Param("url") String url);

    @Query("SELECT c.id FROM Chat c JOIN c.subscriptions s JOIN s.link l WHERE l.url = :url")
    Slice<Long> findAllChatIdsByUrl(@Param("url") String url, Pageable pageable);

    @Modifying
    @Query("UPDATE Link l SET l.lastUpdated = :lastUpdate WHERE l.url = :url")
    void updateLastUpdatedByUrl(@Param("url") String url, @Param("lastUpdate") OffsetDateTime lastUpdateFromApi);

    @Query("SELECT l FROM Link l JOIN l.subscriptions s JOIN s.chat c WHERE c.id = :chatId AND l.url = :url")
    Optional<Link> findByChatIdAndUrl(@Param("chatId") Long chatId, @Param("url") String url);

    @Query("SELECT l FROM Link l WHERE l.url = :url")
    Optional<Link> findByUrl(@Param("url") String url);

    @Modifying
    @Query("UPDATE Link l SET l.lastCheckedAt = :checkedAt WHERE l.id IN :linkIds")
    void updateLastCheckedAt(@Param("linkIds") List<Long> linkIds, @Param("checkedAt") OffsetDateTime checkedAt);

    @Query(value = "SELECT * FROM links ORDER BY checked_at NULLS FIRST", nativeQuery = true)
    Slice<Link> findLinksToCheck(Pageable pageable);

    long countByUrlContaining(String s);
}
