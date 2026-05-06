package backend.academy.linktracker.bot.controller.kafka.repository;

import backend.academy.linktracker.bot.controller.kafka.entity.Event;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRepository extends JpaRepository<Event, UUID> {

    @Modifying
    @Query("DELETE FROM Event e WHERE e.processedAt < :threshold")
    void deleteOldEvents(OffsetDateTime threshold);

    @Modifying
    @Query(
            value =
                    "INSERT INTO processed_event (event_id, processed_at) VALUES (:eventId, :time) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int trySave(@Param("eventId") UUID eventId, @Param("time") OffsetDateTime time);

    void removeEventById(UUID id);
}
