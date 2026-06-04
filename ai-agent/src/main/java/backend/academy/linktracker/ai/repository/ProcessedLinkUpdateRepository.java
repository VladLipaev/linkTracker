package backend.academy.linktracker.ai.repository;

import backend.academy.linktracker.ai.entity.ProcessedLinkUpdate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedLinkUpdateRepository extends JpaRepository<ProcessedLinkUpdate, UUID> {

    @Query(value = """
    SELECT * FROM processed_link_update
    WHERE processed = false
    AND created_at <= :threshold
    and retry_count < 5
    ORDER BY created_at LIMIT :limit
    FOR UPDATE SKIP LOCKED
""", nativeQuery = true)
    List<ProcessedLinkUpdate> findBatchWithSkipLocked(
            @Param("threshold") OffsetDateTime threshold, @Param("limit") int limit);

    @Modifying
    @Query("UPDATE ProcessedLinkUpdate p SET p.processed = true WHERE p.id IN :ids")
    void markProcessed(@Param("ids") List<UUID> ids);

    @Modifying
    @Query(
            value =
                    "DELETE FROM public.processed_link_update WHERE id IN (SELECT id FROM processed_link_update WHERE (processed = true and created_at < :threshold and retry_count > 4)  limit :limit)  ",
            nativeQuery = true)
    int cleanUpBatch(@Param("threshold") OffsetDateTime threshold, @Param("limit") int limit);
}
