package backend.academy.linktracker.scrapper.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record RemoveLinkRequest(
    @URL
    @NotBlank
    String link) {
}
