package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.config.PrioritizationProperties;
import backend.academy.linktracker.ai.entity.dto.Priority;
import backend.academy.linktracker.ai.entity.dto.ProcessedLinkUpdateDto;
import backend.academy.linktracker.scrapper.dto.avro.RawLinkUpdateAvro;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrioritizationService {

    private final PrioritizationProperties prioritizationProperties;

    private Pattern highKeywordsPattern;
    private Pattern lowKeywordsPattern;

    @PostConstruct
    public void init() {
        List<String> highKeywords = prioritizationProperties.highKeywords();
        String highKeywordsRegex = highKeywords.stream()
                .map(word -> "\\b" + Pattern.quote(word) + "\\b")
                .collect(Collectors.joining("|"));
        this.highKeywordsPattern = Pattern.compile(highKeywordsRegex, Pattern.CASE_INSENSITIVE);
        List<String> lowKeywords = prioritizationProperties.lowKeywords();
        String lowKeywordsRegex = lowKeywords.stream()
                .map(word -> "\\b" + Pattern.quote(word) + "\\b")
                .collect(Collectors.joining("|"));
        this.lowKeywordsPattern = Pattern.compile(lowKeywordsRegex, Pattern.CASE_INSENSITIVE);
    }

    public ProcessedLinkUpdateDto prioritize(RawLinkUpdateAvro rawLinkUpdateAvro) {
        String description = rawLinkUpdateAvro.getDescription();
        if (highKeywordsPattern.matcher(description).find()) {
            return new ProcessedLinkUpdateDto(
                    rawLinkUpdateAvro.getId(),
                    rawLinkUpdateAvro.getDescription(),
                    rawLinkUpdateAvro.getTgChatIds(),
                    Priority.HIGH);
        } else if (lowKeywordsPattern.matcher(description).find()) {
            return new ProcessedLinkUpdateDto(
                    rawLinkUpdateAvro.getId(),
                    rawLinkUpdateAvro.getDescription(),
                    rawLinkUpdateAvro.getTgChatIds(),
                    Priority.LOW);
        }
        return new ProcessedLinkUpdateDto(
                rawLinkUpdateAvro.getId(),
                rawLinkUpdateAvro.getDescription(),
                rawLinkUpdateAvro.getTgChatIds(),
                Priority.MEDIUM);
    }
}
