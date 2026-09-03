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
    @Modifying
    @Query("""
update OutBoxMessage o
set o.status = :status, o.processedAt = CURRENT_TIMESTAMP
where o.id = :eventId
and o.status = 'new'
""")
    int updateStatus(UUID eventId, String status);

    long countByStatus(String aNew);

    long count();

//    @Query(value = """
//    SELECT * FROM outbox_link_update
//    WHERE status IN ('new', 'error') AND retry_count < :maxRetries
//    ORDER BY created_at
//    LIMIT :limit
//    FOR UPDATE SKIP LOCKED
//    """, nativeQuery = true)
//    List<OutBoxMessage> findNewWithLock(@Param("limit") Integer limit, @Param("maxRetries") Integer maxRetries);
//
//    @Modifying
//    @Query(
//            value =
//                    "DELETE FROM outbox_link_update WHERE id IN (SELECT id FROM outbox_link_update WHERE ((status = 'sent') or (status = 'error' and retry_count >= 5))  AND processed_at < :threshold LIMIT :limit)",
//            nativeQuery = true)
//    int cleanUpBatch(OffsetDateTime threshold, int limit);
//
//    Optional<OutBoxMessage> findByPartitionKey(String partitionKey);
//
//    @Modifying
//    @Query(value = "update outbox_link_update set status = 'sent', processed_at = :dateTime where id in :sentMessagesIds", nativeQuery = true)
//    void updateSentMessages(List<UUID> sentMessagesIds, OffsetDateTime dateTime);
//
//    @Modifying
//    @Query(value = "update outbox_link_update set status = 'error', retry_count = retry_count + 1, processed_at = :dateTime where id in :errorMessagesIds", nativeQuery = true)
//    void updateErrorMessages(List<UUID> errorMessagesIds, OffsetDateTime dateTime);

    }
