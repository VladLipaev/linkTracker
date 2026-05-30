package backend.academy.linktracker.ai.entity;

import backend.academy.linktracker.ai.entity.dto.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "processed_link_update")
@Entity
public class ProcessedLinkUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_event_id")
    private Long originalEventId;

    @Column
    private String description;

    @Column(name = "tg_chat_id")
    private Long tgChatId;

    @Column
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "processed")
    private boolean processed;

    @Column
    private Integer retryCount;
}
