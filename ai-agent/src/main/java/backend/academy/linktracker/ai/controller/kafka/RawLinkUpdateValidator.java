package backend.academy.linktracker.ai.controller.kafka;

import backend.academy.linktracker.ai.config.FilteringProperties;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RawLinkUpdateValidator {

    private final FilteringProperties filteringProperties;

    private Pattern stopWordsPattern;

    @Value("${app.summarization.threshold:500}")
    private Integer threshold;

    @PostConstruct
    public void init() {
        List<String> stopWords = filteringProperties.stopWords();
        if (stopWords != null && !stopWords.isEmpty()) {
            String regex = stopWords.stream()
                    .map(word -> "\\b" + Pattern.quote(word) + "\\b")
                    .collect(Collectors.joining("|"));
            this.stopWordsPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
    }

    public boolean validate(RawLinkUpdateAvro rawLinkUpdateAvro) {
        if (rawLinkUpdateAvro == null) {
            return false;
        }

        String author = rawLinkUpdateAvro.getAuthor();
        String description = rawLinkUpdateAvro.getDescription();

        if (author == null || description == null) {
            return false;
        }

        List<String> excludedAuthors = filteringProperties.excludedAuthors();
        Integer minLength = filteringProperties.minLength();

        if (excludedAuthors != null && excludedAuthors.contains(author)) {
            return false;
        }

        if (minLength != null && description.length() <= minLength) {
            return false;
        }

        return stopWordsPattern == null
                || !stopWordsPattern.matcher(description).find();
    }

    public boolean isAboveThreshold(RawLinkUpdateAvro rawLinkUpdateAvro) {
        return rawLinkUpdateAvro.getDescription().length() > threshold;
    }
}
