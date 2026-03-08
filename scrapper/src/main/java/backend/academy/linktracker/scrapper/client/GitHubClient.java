package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.GitHubRepoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class GitHubClient {

    private final RestClient restClient;

    public GitHubRepoResponse fetchRepo(String owner, String repo) {
        return restClient
                .get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new RuntimeException("GitHub API error: " + response.getStatusCode());
                })
                .body(GitHubRepoResponse.class);
    }
}
