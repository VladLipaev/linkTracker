package backend.academy.linktracker.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowResponse(
    List<Item> items
) {

    public record Item(
        @JsonProperty("last_activity_date")
        long lastActivityDate) {
    }

}
