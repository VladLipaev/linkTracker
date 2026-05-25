package backend.academy.linktracker.scrapper.handler;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.handler.dto.GitHubIssueResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitHubLinkHandler implements LinkHandler {

    private static final Pattern PATTERN =
            Pattern.compile("^https://github\\.com/(?<owner>[\\w.-]+)/(?<repo>[\\w.-]+)/?$");

    private final GitHubClient gitHubClient;

    @Value("${app.github.per-page}")
    private Integer perPage;

    @Override
    public boolean supports(String url) {
        return PATTERN.matcher(url).matches();
    }

    @Override
    public UpdateResult fetchUpdate(String url, OffsetDateTime lastUpdated) throws IllegalArgumentException {
        Matcher matcher = PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Ссылка не соответствует формату GitHub");
        }
        String owner = matcher.group("owner");
        String repo = matcher.group("repo");
        int page = 1;
        boolean hasMoreIssues = true;
        boolean hasNewItems = false;
        OffsetDateTime maxUpdate = lastUpdated;
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder
                .append("Обновления в репозитории: *")
                .append(owner)
                .append("/")
                .append(repo)
                .append("*\n\n");

        while (hasMoreIssues) {
            List<GitHubIssueResponse> updates = gitHubClient.fetchRepo(owner, repo, lastUpdated, page, perPage);

            if (updates == null || updates.isEmpty()) {
                return new UpdateResult(false, lastUpdated, null);
            }

            for (GitHubIssueResponse item : updates) {

                if (!item.updatedAt().isAfter(lastUpdated)) {
                    continue;
                }

                if (item.updatedAt().isAfter(maxUpdate)) {
                    maxUpdate = item.updatedAt();
                }
                if (item.createdAt().isAfter(lastUpdated)) {
                    hasNewItems = true;

                    String type = item.isPullRequest() ? "Pull Request" : "Issue";
                    String safeBody = (item.body() != null && !item.body().isBlank()) ? item.body() : "Нет описания";

                    messageBuilder
                            .append("**Новый ")
                            .append(type)
                            .append(":** ")
                            .append(item.title())
                            .append("\n");
                    messageBuilder.append("Автор: ").append(item.user().login()).append("\n");
                    messageBuilder
                            .append("Время создания: ")
                            .append(item.createdAt())
                            .append("\n");
                    messageBuilder.append("Описание: ").append(safeBody).append("\n\n");
                }
            }

            if (updates.size() < perPage) {
                hasMoreIssues = false;
            } else {
                page++;
            }
        }

        if (hasNewItems) {
            return new UpdateResult(true, maxUpdate, messageBuilder.toString());
        } else {
            return new UpdateResult(maxUpdate.isAfter(lastUpdated), maxUpdate, null);
        }
    }
}
