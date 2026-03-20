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

    @Query("SELECT c.id from Chat c join c.links l where l.url = :url")
    List<Long> findAllChatIdsByUrl(@Param("url") String url);

    @Query("SELECT c.id from Chat c join c.links l where l.url = :url")
    Slice<Long> findAllChatIdsByUrl(@Param("url") String url, Pageable pageable);

    @Modifying
    @Query("UPDATE Link l set l.lastUpdated = :lastUpdate where l.url = :url")
    void updateLastUpdatedByUrl(@Param("url") String url, @Param("lastUpdate") OffsetDateTime lastUpdateFromApi);

    @Query("SELECT l FROM Link l join l.chats c where c.id = :chatId and l.url = :url")
    Optional<Link> findByChatIdAndUrl(@Param("chatId") Long chatId, @Param("url") String url);

    @Query("SELECT l FROM Link l where l.url = :url")
    Optional<Link> findByUrl(@Param("url") String url);
}
