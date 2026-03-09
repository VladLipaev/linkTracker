package backend.academy.linktracker.bot.config;

import backend.academy.linktracker.bot.client.scrapper.RestClientScrapperRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class TestingBeans {

    @Bean
    @Primary
    public RestClientScrapperRestClient restClient(
            @Value("${app.scrapper.uri:http://localhost:54321}") String scrapperBaseUri) {
        return new RestClientScrapperRestClient(
                RestClient.builder().baseUrl(scrapperBaseUri).build());
    }
}
