package backend.academy.linktracker.scrapper.entity;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "outbox_link_update")
public class OutBoxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregatetype", nullable = false)
    private String aggregateType;

    @Column(name = "aggregateid", nullable = false)
    private String aggregateId;

    @Column(name = "eventtype", nullable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "JSON", nullable = false)
    private RawLinkUpdateAvro payload;

    @Column(name = "partition_key")
    private String partitionKey;

    @Column(name = "status", nullable = false)
    private String status = "new";

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "span_id", nullable = false)
    private String spanId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Builder
    public OutBoxMessage(UUID uuid, RawLinkUpdateAvro payload, String partitionKey, String traceId, String spanId, String aggregateType, String aggregateId, String eventType) {
        this.id = uuid;
        this.payload = payload;
        this.partitionKey = partitionKey;
        this.traceId = traceId;
        this.spanId = spanId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
    }
}
