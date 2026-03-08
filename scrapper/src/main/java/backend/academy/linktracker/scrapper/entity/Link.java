package backend.academy.linktracker.scrapper.entity;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Link {
    private Long id;
    private Long chatId;
    private String url;
    private List<String> tags;
    private OffsetDateTime lastUpdated;
}
