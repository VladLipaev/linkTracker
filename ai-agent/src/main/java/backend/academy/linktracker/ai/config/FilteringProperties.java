package backend.academy.linktracker.ai.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.filtering", ignoreUnknownFields = false)
public record FilteringProperties(List<String> stopWords, List<String> excludedAuthors, Integer minLength) {}
