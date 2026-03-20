package backend.academy.linktracker.scrapper.handler;

import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.StackOverflowResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackoverflowLinkHandler implements LinkHandler {

    private final StackOverflowClient stackOverflowClient;
    private static final Pattern PATTERN = Pattern.compile("^https://stackoverflow\\.com/questions/(?<id>\\d+)(/.*)?$");

    @Override
    public boolean supports(String url) {
        return PATTERN.matcher(url).matches();
    }

    @Override
    public OffsetDateTime fetchUpdate(String url) throws IllegalArgumentException {
        Matcher matcher = PATTERN.matcher(url);
        if (matcher.matches()) {
            String questionId = matcher.group("id");
            StackOverflowResponse response = stackOverflowClient.fetchQuestion(questionId);

            if (response != null
                    && response.items() != null
                    && !response.items().isEmpty()) {
                long seconds = response.items().getFirst().lastActivityDate();

                return Instant.ofEpochSecond(seconds).atOffset(ZoneOffset.UTC);
            }
        }
        throw new IllegalArgumentException("По ссылке %s вопроса на StackOverFlow не найдено".formatted(url));
    }
}
