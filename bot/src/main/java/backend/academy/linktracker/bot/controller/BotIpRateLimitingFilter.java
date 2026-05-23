package backend.academy.linktracker.bot.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BotIpRateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final Cache<String, RateLimiter> rateLimiterCache;

    public BotIpRateLimitingFilter(
            RateLimiterRegistry rateLimiterRegistry,
            @Value("${app.rate-limiter.api.caffeine.size}") Integer maxSize,
            @Value("${app.rate-limiter.api.caffeine.expire-time}") Duration expireTime) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.rateLimiterCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expireTime)
                .removalListener(this::onCacheRemoval)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = getClientIp(request);

        RateLimiter rateLimiter = rateLimiterCache.get(clientIp, ip -> rateLimiterRegistry.rateLimiter(ip, "api"));
        if (rateLimiter != null && rateLimiter.acquirePermission()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests\"}");
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void onCacheRemoval(String key, RateLimiter rateLimiter, RemovalCause cause) {
        if (key != null) {
            rateLimiterRegistry.remove(key);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
