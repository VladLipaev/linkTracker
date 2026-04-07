package backend.academy.linktracker.scrapper.config;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.bot.RestClientTelegramBotRestClient;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientBeans {

    @Bean
    @ConditionalOnProperty(prefix = "app.communication", name = "mode", havingValue = "rest", matchIfMissing = true)
    public RestClientTelegramBotRestClient restClientTelegramBotRestClient(
            @Value("${app.bot.uri:http://localhost:8080}") String botBaseUri) {
        return new RestClientTelegramBotRestClient(
                RestClient.builder().baseUrl(botBaseUri).build());
    }

    @Bean
    public GitHubClient gitHubClient(
            GithubProperties githubProperties, @Value("${app.github.base-url}") String baseUrl) {
        return new GitHubClient(RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + githubProperties.getToken())
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build());
    }

    @Bean
    public StackOverflowClient stackOverflowClient(
            StackoverflowProperties stackoverflowProperties, @Value("${app.stackoverflow.base-url}") String baseUrl) {
        return new StackOverflowClient(RestClient.builder().baseUrl(baseUrl).build(), stackoverflowProperties);
    }
}
