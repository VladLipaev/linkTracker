package backend.academy.linktracker.ai.config;

import backend.academy.linktracker.ai.client.YandexGPTRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientBeans {

    @Bean
    public YandexGPTRestClient yandexGPTRestClient() {
        return new YandexGPTRestClient(
                RestClient.builder().baseUrl("https://llm.api.cloud.yandex.net").build());
    }
}
