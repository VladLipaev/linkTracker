package backend.academy.linktracker.scrapper.repository.outbox;

import backend.academy.linktracker.scrapper.entity.OutBoxMessage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutBoxRepository extends JpaRepository<OutBoxMessage, UUID> {

    @Query(value = """
    SELECT * FROM outbox_link_update
    WHERE status IN ('new', 'error') AND retry_count < 5
    ORDER BY created_at
    LIMIT :limit
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<OutBoxMessage> findNewWithLock(@Param("limit") int limit);

    // ERROR'Ы пока что НЕ УДАЛЯЛ
    @Modifying
    @Query(
            value =
                    "DELETE FROM outbox_link_update WHERE id IN (SELECT id FROM outbox_link_update WHERE ((status = 'sent') or (status = 'error' and retry_count >= 5))  AND processed_at < :threshold LIMIT :limit)",
            nativeQuery = true)
    int cleanUpBatch(OffsetDateTime threshold, int limit);

    Optional<OutBoxMessage> findByPartitionKey(String partitionKey);
}
