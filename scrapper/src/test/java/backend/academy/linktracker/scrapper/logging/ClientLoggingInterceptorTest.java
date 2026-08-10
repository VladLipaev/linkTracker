package backend.academy.linktracker.scrapper.logging;

import backend.academy.linktracker.scrapper.config.logging.ClientLoggingInterceptor;
import backend.academy.linktracker.scrapper.properties.GithubMaskingProperties;
import backend.academy.linktracker.scrapper.properties.HttpMaskingProperties;
import backend.academy.linktracker.scrapper.properties.SOMaskingProperties;
import backend.academy.linktracker.scrapper.service.MaskingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientLoggingInterceptorTest {

    @Mock
    private MaskingService maskingService;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    private ClientLoggingInterceptor interceptor;

    private GithubMaskingProperties githubProperties = new GithubMaskingProperties();
    private SOMaskingProperties soProperties = new SOMaskingProperties();
    private HttpMaskingProperties httpProperties = new HttpMaskingProperties();

    @BeforeEach
    void setUp() {
        interceptor = new ClientLoggingInterceptor(
            maskingService,
            githubProperties,
            soProperties,
            httpProperties
        );

        ReflectionTestUtils.setField(interceptor, "githubUrl", "/github");
        ReflectionTestUtils.setField(interceptor, "soUrl", "/stackoverflow");
    }

    @Test
    @DisplayName("Перехватчик должен использовать GithubMaskingProperties для запросов к GitHub")
    void shouldApplyGithubMaskingRules() throws IOException {
        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        URI uri = URI.create("https://api.github.com/github/repos?token=secret");

        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        when(execution.execute(any(), any())).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        interceptor.intercept(request, new byte[0], execution);

        // Проверяем, что маскирование вызвалось именно с githubProperties
        verify(maskingService).maskQuery(eq("token=secret"), eq(githubProperties));
        verify(maskingService).maskHeaders(eq(headers), eq(githubProperties));
    }
}
