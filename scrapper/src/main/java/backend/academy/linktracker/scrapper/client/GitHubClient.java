package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.GitHubRepoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
public class GitHubClient {

    private final RestClient restClient;

    public GitHubRepoResponse fetchRepo(String owner, String repo) {
        try {
            GitHubRepoResponse response = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        throw new GitHubClientException("GitHub API error: " + res.getStatusCode());
                    })
                    .body(GitHubRepoResponse.class);
            if (response == null || response.updatedAt() == null) {
                throw new GitHubClientException(
                        "GitHub API error: Тело ответа не соответствует заявленной схеме (отсутствует updatedAt)");
            }
            return response;
        } catch (RestClientException e) {
            throw new GitHubClientException("Github API error: " + e.getMessage());
        }
    }
}
