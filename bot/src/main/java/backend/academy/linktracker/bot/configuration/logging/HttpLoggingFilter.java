package backend.academy.linktracker.bot.configuration.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class HttpLoggingFilter extends OncePerRequestFilter {

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
        String query = request.getQueryString();
        log.info("Request: method: {}, URI: {}, query: {}", method, URI, query);
    }

    private void logResponse(HttpServletResponse response, long duration){
        int status = response.getStatus();
        log.info("Response: status: {}, duration: {}", status, duration);
    }
}
