package backend.academy.linktracker.ai.config.logging;

import backend.academy.linktracker.ai.config.logging.properties.HttpMaskingProperties;
import backend.academy.linktracker.ai.service.MaskingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class HttpLoggingFilter extends OncePerRequestFilter {


    private final MaskingService maskingService;
    private final HttpMaskingProperties httpMaskingProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        try{
            filterChain.doFilter(request, response);
        }
        finally {
            long duration = System.currentTimeMillis() - start;

            logRequest(request);

            logResponse(response, duration);
        }


    }

    private void logRequest(HttpServletRequest request){
        String method = request.getMethod();
        String URI = request.getRequestURI();
        String query = maskingService.maskQuery(request.getQueryString(), httpMaskingProperties);
        log.info("Request: method: {}, URI: {}, query: {}", method, URI, query);
    }

    private void logResponse(HttpServletResponse response, long duration){
        int status = response.getStatus();
        log.info("Response: status: {}, duration: {}", status, duration);
    }
}
