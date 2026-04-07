package backend.academy.linktracker.scrapper.handler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record GitHubIssueResponse(
        @JsonProperty("title") String title,
        @JsonProperty("user") User user,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("body") String body,
        @JsonProperty("pull_request") Object pullRequest) {

    public record User(@JsonProperty("login") String login) {}

    public boolean isPullRequest() {
        return pullRequest != null;
    }
}
