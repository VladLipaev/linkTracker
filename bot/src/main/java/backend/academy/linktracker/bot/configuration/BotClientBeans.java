package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.client.scrapper.RestClientScrapperRestClient;
import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

@Configuration
public class BotClientBeans {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.communication.client",
            name = "mode",
            havingValue = "rest",
            matchIfMissing = true)
    public ScrapperClient restClientScrapperRestClient(
            @Value("${app.scrapper.uri:http://localhost:8081}") String scrapperBaseUri,
            @Value("${app.communication.client.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.communication.client.read-timeout:10s}") Duration readTimeout,
            KafkaTemplate<String, Object> kafkaTemplate) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestClientScrapperRestClient(
                RestClient.builder()
                        .baseUrl(scrapperBaseUri)
                        .requestFactory(factory)
                        .build(),
                kafkaTemplate);
    }
}
