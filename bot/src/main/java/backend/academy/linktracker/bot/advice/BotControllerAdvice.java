package backend.academy.linktracker.bot.advice;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class BotControllerAdvice {

    private ResponseEntity<ApiErrorResponse> createResponse(Exception ex, HttpStatus status, String description) {
        ApiErrorResponse error = new ApiErrorResponse(
                description,
                String.valueOf(status.value()),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                Arrays.stream(ex.getStackTrace())
                        .map(StackTraceElement::toString)
                        .toList());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RequestNotPermitted ex) {
        log.atError()
                .setMessage("превышен лимит запросов")
                .addKeyValue("error.message", ex.getMessage())
                .log();

        return createResponse(ex, HttpStatus.TOO_MANY_REQUESTS, "превышен лимит запросов");
    }
}
