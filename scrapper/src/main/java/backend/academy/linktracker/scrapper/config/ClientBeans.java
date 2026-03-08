package backend.academy.linktracker.scrapper.config;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.RestClientTelegramBotRestClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientBeans {

    @Bean
    public RestClientTelegramBotRestClient restClientTelegramBotRestClient(
            @Value("${app.bot.uri:http://localhost:8080}") String botBaseUri) {
        return new RestClientTelegramBotRestClient(
                RestClient.builder().baseUrl(botBaseUri).build());
    }

    @Bean
    public GitHubClient gitHubClient(GithubProperties githubProperties) {
        return new GitHubClient(RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubProperties.getToken())
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build());
    }

    @Bean
    public StackOverflowClient stackOverflowClient(StackoverflowProperties stackoverflowProperties) {
        return new StackOverflowClient(
                RestClient.builder()
                        .baseUrl("https://api.stackexchange.com/2.3")
                        .build(),
                stackoverflowProperties);
    }
}
