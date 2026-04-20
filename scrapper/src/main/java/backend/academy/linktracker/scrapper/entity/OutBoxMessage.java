package backend.academy.linktracker.scrapper.entity;

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
@Table(name = "outbox_link_update")
public class OutBoxMessage {

    @Id
    private UUID id;

    @Column(name = "payload")
    private String payload;

    @Column(name = "partition_key")
    private String partitionKey;

    @Column(name = "status")
    private String status = "new";

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public OutBoxMessage(UUID uuid, String payload, String partitionKey) {
        this.id = uuid;
        this.payload = payload;
        this.partitionKey = partitionKey;
    }
}
