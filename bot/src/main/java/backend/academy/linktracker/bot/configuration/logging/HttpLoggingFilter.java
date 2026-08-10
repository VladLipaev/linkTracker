package backend.academy.linktracker.bot.configuration.logging;

import backend.academy.linktracker.bot.properties.HttpMaskingProperties;
import backend.academy.linktracker.bot.service.MaskingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@RequiredArgsConstructor
public class HttpLoggingFilter extends OncePerRequestFilter {

    private final MaskingService maskingService;
    private final HttpMaskingProperties httpMaskingProperties;

    @Value("${logging.cache.limit:4096}")
    private int cacheLimit;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, cacheLimit);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - start;

            logRequest(requestWrapper);
            logResponse(responseWrapper, duration);

            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = maskingService.maskQuery(request.getQueryString(), httpMaskingProperties);
        byte[] content = request.getContentAsByteArray();
        String body = content.length > 0 ? new String(content, StandardCharsets.UTF_8) : "";
        String maskedBody = maskingService.maskBody(body, httpMaskingProperties);
        log.info("Request: method: {}, URI: {}, query: {}, body: {}", method, uri, query, maskedBody);
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        byte[] content = response.getContentAsByteArray();
        String body = content.length > 0 ? new String(content, StandardCharsets.UTF_8) : "";
        String maskedBody = maskingService.maskBody(body, httpMaskingProperties);
        log.info("Response: status: {}, duration: {}ms, body: {}", status, duration, maskedBody);
    }
}
