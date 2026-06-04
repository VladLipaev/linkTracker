package backend.academy.linktracker.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "raw_processed_event")
public class Event {

    @Id
    @Column(name = "event_id")
    private UUID id;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;
}
