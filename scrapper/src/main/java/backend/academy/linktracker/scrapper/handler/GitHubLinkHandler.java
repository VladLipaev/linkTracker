package backend.academy.linktracker.scrapper.handler;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitHubLinkHandler implements LinkHandler {

    private static final Pattern PATTERN =
            Pattern.compile("^https://github\\.com/(?<owner>[\\w.-]+)/(?<repo>[\\w.-]+)/?$");

    private final GitHubClient gitHubClient;

    @Override
    public boolean supports(String url) {
        return PATTERN.matcher(url).matches();
    }

    @Override
    public OffsetDateTime fetchUpdate(String url) throws IllegalArgumentException {
        Matcher matcher = PATTERN.matcher(url);
        if (matcher.matches()) {
            String owner = matcher.group("owner");
            String repo = matcher.group("repo");

            return gitHubClient.fetchRepo(owner, repo).updatedAt();
        }
        throw new IllegalArgumentException("Ссылка не соответствует формату GitHub");
    }
}
