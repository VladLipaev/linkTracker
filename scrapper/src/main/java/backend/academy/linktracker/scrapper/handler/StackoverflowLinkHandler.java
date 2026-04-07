package backend.academy.linktracker.scrapper.handler;

import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.handler.dto.StackOverflowResponse;
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
    public UpdateResult fetchUpdate(String url, OffsetDateTime lastUpdated) throws IllegalArgumentException {
        Matcher matcher = PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("По ссылке %s вопроса на StackOverFlow не найдено".formatted(url));
        }
        String questionId = matcher.group("id");
        var qResponse = stackOverflowClient.fetchQuestion(questionId);
        if (qResponse.items() == null || qResponse.items().isEmpty()) {
            return new UpdateResult(false, lastUpdated, null);
        }
        var question = qResponse.items().getFirst();
        OffsetDateTime apiLastActivity =
                Instant.ofEpochSecond(question.lastActivityDate()).atOffset(ZoneOffset.UTC);
        if (!apiLastActivity.isAfter(lastUpdated)) {
            return new UpdateResult(false, lastUpdated, null);
        }
        long fromDateSec = lastUpdated.toEpochSecond();
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Обновление в теме: *").append(question.title()).append("*\n\n");
        boolean hasActualUpdates = false;

        var answers = stackOverflowClient.fetchAnswers(questionId, fromDateSec);
        if (answers != null && answers.items() != null) {
            hasActualUpdates = true;
            for (var answer : answers.items()) {
                appendUpdateText(messageBuilder, "Новый ответ", answer);
            }
        }
        var comments = stackOverflowClient.fetchComments(questionId, fromDateSec);
        if (comments != null && comments.items() != null) {
            for (var comment : comments.items()) {
                hasActualUpdates = true;
                appendUpdateText(messageBuilder, "Новый комментарий", comment);
            }
        }
        if (hasActualUpdates) {
            return new UpdateResult(true, apiLastActivity, messageBuilder.toString());
        } else {
            return new UpdateResult(true, apiLastActivity, "Вопрос был отредактирован: " + question.title());
        }
    }

    private void appendUpdateText(StringBuilder sb, String type, StackOverflowResponse.ActivityItem item) {
        String safeBody = (item.body() != null) ? item.body().replaceAll("<[^>]*>", "") : "";
        String preview = safeBody.length() > 200 ? safeBody.substring(0, 200) + "..." : safeBody;

        sb.append("**")
                .append(type)
                .append("** от ")
                .append(item.owner().displayName())
                .append("\n");
        sb.append("Время: ").append(item.getCreationDateFormatted()).append("\n");
        sb.append("Превью: ").append(preview).append("\n\n");
    }
}
