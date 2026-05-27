package backend.academy.linktracker.ai.config;

import backend.academy.linktracker.ai.client.YandexGPTRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientBeans {

    @Bean
    public YandexGPTRestClient yandexGPTRestClient(
            @Value("${app.yandexgpt.uri:https://llm.api.cloud.yandex.net}") String baseUri) {
        return new YandexGPTRestClient(RestClient.builder().baseUrl(baseUri).build());
    }
}
