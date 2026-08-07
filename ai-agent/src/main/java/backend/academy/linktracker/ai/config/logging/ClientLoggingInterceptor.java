package backend.academy.linktracker.ai.config.logging;

import java.io.IOException;
import backend.academy.linktracker.ai.config.logging.properties.HttpMaskingProperties;
import backend.academy.linktracker.ai.config.logging.properties.YandexMaskingProperties;
import backend.academy.linktracker.ai.service.MaskingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    private final MaskingService maskingService;
    private final HttpMaskingProperties httpMaskingProperties;
    private final YandexMaskingProperties yandexMaskingProperties;

    @Value("${app.yandexgpt.uri}")
    private String yandexUri;


    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException, IOException {
        long start = System.currentTimeMillis();

        String query = request.getURI().getQuery();
        String uri = request.getURI().getPath();
        HttpHeaders headers = request.getHeaders();
        String maskQuery;
        String maskedHeaders;
        if (uri.startsWith(yandexUri)){
            maskQuery = query;
            maskedHeaders = maskingService.maskHeaders(headers, yandexMaskingProperties);
        }
        else{
            maskQuery = maskingService.maskQuery(query, httpMaskingProperties);
            maskedHeaders = maskingService.maskHeaders(headers, httpMaskingProperties);
        }
        log.info("Outgoing Request: method: {}, URI: {},  query: {}, headers: {}",
            request.getMethod(), uri, maskQuery, maskedHeaders);

        ClientHttpResponse response = execution.execute(request, body);
        long duration = System.currentTimeMillis() - start;

        log.info("Outgoing Response: status={}, duration={}ms", response.getStatusCode().value(), duration);
        return response;
    }

}
