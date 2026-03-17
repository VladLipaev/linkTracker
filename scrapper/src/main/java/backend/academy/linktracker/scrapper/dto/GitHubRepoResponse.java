package backend.academy.linktracker.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record GitHubRepoResponse(
    @JsonProperty("updated_at")
    OffsetDateTime updatedAt) {
}
