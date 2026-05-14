package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.handler.dto.GitHubIssueResponse;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
@Slf4j
public class GitHubClient {

    private final RestClient restClient;

    @Retry(name = "external")
    public List<GitHubIssueResponse> fetchRepo(String owner, String repo, OffsetDateTime since, int page, int perPage)
            throws GitHubClientException {
        try {
            List<GitHubIssueResponse> response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues")
                            .queryParam("state", "all")
                            .queryParam("since", since.toString())
                            .queryParam("per_page", perPage)
                            .queryParam("page", page)
                            .build(owner, repo))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        throw new GitHubClientException("GitHub API error: " + res.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                throw new GitHubClientException(
                        "GitHub API error: Тело ответа не соответствует заявленной схеме (отсутствует updatedAt)");
            }

            ClientRequestLogging.handleRequestSuccess("Успешный запрос в github", "github_request", "success");
            return response;

        } catch (RestClientException e) {
            ClientRequestLogging.handleRequestFailure("Неудачный запрос в github", "github_request", "failure", e);
            throw new GitHubClientException("Github API error: " + e.getMessage());
        }
    }
}
