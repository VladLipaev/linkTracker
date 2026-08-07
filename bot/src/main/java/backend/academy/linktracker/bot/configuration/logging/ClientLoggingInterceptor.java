package backend.academy.linktracker.bot.configuration.logging;

import backend.academy.linktracker.bot.properties.HttpMaskingProperties;
import backend.academy.linktracker.bot.properties.TelegramMaskingProperties;
import backend.academy.linktracker.bot.service.MaskingService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final TelegramMaskingProperties telegramMaskingProperties;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException, IOException {
        long start = System.currentTimeMillis();

        String query = request.getURI().getQuery();
        String uri = request.getURI().getPath();
        HttpHeaders headers = request.getHeaders();
        String maskQuery;
        String maskedHeaders;
        if (uri.startsWith("https://api.telegram.org/bot")){
            uri = maskingService.maskURI(uri, telegramMaskingProperties);
            maskQuery = maskingService.maskQuery(query, telegramMaskingProperties);
            maskedHeaders = maskingService.maskHeaders(headers, telegramMaskingProperties);
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
