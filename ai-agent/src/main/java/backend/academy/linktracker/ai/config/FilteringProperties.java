package backend.academy.linktracker.ai.config;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.filtering", ignoreUnknownFields = false)
public record FilteringProperties(
        @NotNull List<String> stopWords,
        @NotNull List<String> excludedAuthors,
        @NotNull Integer minLength) {}
