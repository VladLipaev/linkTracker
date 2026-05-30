package backend.academy.linktracker.ai.repository;

import backend.academy.linktracker.ai.entity.OutboxProcessedMessage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxProcessedMessage, UUID> {

    @Query(value = """
    SELECT * FROM public.outbox_processed_link_update
    WHERE status IN ('new', 'error') AND retry_count < :maxRetries
    ORDER BY created_at
    LIMIT :limit
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<OutboxProcessedMessage> findNewWithLock(
            @Param("limit") Integer limit, @Param("maxRetries") Integer maxRetries);

    @Modifying
    @Query(
            value =
                    "DELETE FROM public.outbox_processed_link_update WHERE id IN (SELECT id FROM outbox_processed_link_update WHERE ((status = 'sent') or (status = 'error' and retry_count >= 5))  AND processed_at < :threshold LIMIT :limit)",
            nativeQuery = true)
    int cleanUpBatch(OffsetDateTime threshold, int limit);
}
