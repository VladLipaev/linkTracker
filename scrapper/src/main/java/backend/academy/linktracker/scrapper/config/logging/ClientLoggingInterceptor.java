package backend.academy.linktracker.scrapper.config.logging;

import backend.academy.linktracker.scrapper.properties.GithubMaskingProperties;
import backend.academy.linktracker.scrapper.properties.HttpMaskingProperties;
import backend.academy.linktracker.scrapper.properties.SOMaskingProperties;
import backend.academy.linktracker.scrapper.service.MaskingService;
import java.io.IOException;
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
    private final GithubMaskingProperties githubMaskingProperties;
    private final SOMaskingProperties soMaskingProperties;
    private final HttpMaskingProperties httpMaskingProperties;

    @Value("${app.github.base-url}")
    private String githubUrl;

    @Value("${app.stackoverflow.base-url}")
    private String soUrl;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException, IOException {
        long start = System.currentTimeMillis();

        String query = request.getURI().getQuery();
        String uri = request.getURI().getPath();
        HttpHeaders headers = request.getHeaders();
        String maskQuery;
        String maskedHeaders;
        if (uri.startsWith(githubUrl)) {
            maskQuery = maskingService.maskQuery(query, githubMaskingProperties);
            maskedHeaders = maskingService.maskHeaders(headers, githubMaskingProperties);
        } else if (uri.startsWith(soUrl)) {
            maskQuery = maskingService.maskQuery(query, soMaskingProperties);
            maskedHeaders = maskingService.maskHeaders(headers, soMaskingProperties);
        } else {
            maskQuery = maskingService.maskQuery(query, httpMaskingProperties);
            maskedHeaders = maskingService.maskHeaders(headers, httpMaskingProperties);
        }
        log.info("Outgoing Request: method: {}, URI: {},  query: {}, headers: {}",
            request.getMethod(), request.getURI().getPath(), maskQuery, maskedHeaders);

        ClientHttpResponse response = execution.execute(request, body);
        long duration = System.currentTimeMillis() - start;

        log.info("Outgoing Response: status={}, duration={}ms", response.getStatusCode().value(), duration);
        return response;
    }
}
