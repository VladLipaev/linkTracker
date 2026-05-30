package backend.academy.linktracker.ai.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.prioritization", ignoreUnknownFields = false)
public record PrioritizationProperties(
        @NotNull @NotEmpty List<String> highKeywords,
        @NotNull @NotEmpty List<String> lowKeywords) {}
