package backend.academy.linktracker.scrapper.handler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public record StackOverflowResponse<T>(List<T> items) {

    public record QuestionItem(
            @JsonProperty("last_activity_date") long lastActivityDate,
            @JsonProperty("title") String title) {}

    public record ActivityItem(
            @JsonProperty("creation_date") long creationDate,
            @JsonProperty("body_markdown") String body,
            @JsonProperty("owner") Owner owner) {
        public String getCreationDateFormatted() {
            return Instant.ofEpochSecond(creationDate).atOffset(ZoneOffset.UTC).toString();
        }
    }

    public record Owner(@JsonProperty("display_name") String displayName) {}
}
