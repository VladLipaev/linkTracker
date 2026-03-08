package backend.academy.linktracker.scrapper.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record AddLinkRequest(@NotBlank @URL String link, List<String> tags) {}
