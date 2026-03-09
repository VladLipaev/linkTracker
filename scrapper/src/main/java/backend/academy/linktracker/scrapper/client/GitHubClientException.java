package backend.academy.linktracker.scrapper.client;

public class GitHubClientException extends RuntimeException {
    public GitHubClientException(String message) {
        super(message);
    }
}
