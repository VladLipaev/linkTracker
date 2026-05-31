package backend.academy.linktracker.scrapper.config;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.bot.RestClientTelegramBotClient;
import backend.academy.linktracker.scrapper.config.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import backend.academy.linktracker.scrapper.service.kafka.KafkaNotificationUpdateSender;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ScrapperClientBeans {

    @Bean
    @ConditionalOnProperty(prefix = "app.communication.client", name = "mode", havingValue = "rest")
    public RestClientTelegramBotClient restClientTelegramBotRestClient(
            @Value("${app.bot.uri:http://localhost:8080}") String botBaseUri,
            KafkaNotificationUpdateSender updateSender,
            @Value("${app.communication.client.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.communication.client.read-timeout:10s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setReadTimeout(readTimeout);
        clientHttpRequestFactory.setConnectTimeout(connectTimeout);
        return new RestClientTelegramBotClient(
                RestClient.builder()
                        .requestFactory(clientHttpRequestFactory)
                        .baseUrl(botBaseUri)
                        .build(),
                updateSender);
    }

    @Bean
    public GitHubClient gitHubClient(
            GithubProperties githubProperties,
            @Value("${app.github.base-url}") String baseUrl,
            @Value("${app.communication.client.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.communication.client.read-timeout:10s}") Duration readTimeout,
            ScrapperMetrics scrapperMetrics) {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(connectTimeout);
        simpleClientHttpRequestFactory.setReadTimeout(readTimeout);
        return new GitHubClient(
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeader("Authorization", "Bearer " + githubProperties.getToken())
                        .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                        .requestFactory(simpleClientHttpRequestFactory)
                        .build(),
                scrapperMetrics);
    }

    @Bean
    public StackOverflowClient stackOverflowClient(
            StackoverflowProperties stackoverflowProperties,
            @Value("${app.stackoverflow.base-url}") String baseUrl,
            @Value("${app.communication.client.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.communication.client.read-timeout:10s}") Duration readTimeout,
            ScrapperMetrics scrapperMetrics) {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(connectTimeout);
        simpleClientHttpRequestFactory.setReadTimeout(readTimeout);
        return new StackOverflowClient(
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .requestFactory(simpleClientHttpRequestFactory)
                        .build(),
                stackoverflowProperties,
                scrapperMetrics);
    }
}
