package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.client.scrapper.RestClientScrapperRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientBeans {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.communication.client",
            name = "mode",
            havingValue = "rest",
            matchIfMissing = true)
    public RestClientScrapperRestClient restClientScrapperRestClient(
            @Value("${app.scrapper.uri:http://localhost:8081}") String scrapperBaseUri) {
        return new RestClientScrapperRestClient(
                RestClient.builder().baseUrl(scrapperBaseUri).build());
    }
}
