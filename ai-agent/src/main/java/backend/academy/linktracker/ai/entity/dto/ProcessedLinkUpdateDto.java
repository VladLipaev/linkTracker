package backend.academy.linktracker.ai.entity.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedLinkUpdateDto {
    private Long id;
    private String description;
    private List<Long> tgChatIds;
    private Priority priority;
}
