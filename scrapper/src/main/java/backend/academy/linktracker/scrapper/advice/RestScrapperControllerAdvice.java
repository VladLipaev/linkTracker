package backend.academy.linktracker.scrapper.advice;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.service.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.service.ChatNotFoundException;
import backend.academy.linktracker.scrapper.service.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.service.UnsupportedLinkException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.Arrays;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class RestScrapperControllerAdvice {
    @ExceptionHandler(ChatAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleChatAlreadyExists(ChatAlreadyExistsException ex) {
        return createResponse(ex, HttpStatus.CONFLICT, "Чат уже существует");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(Exception ex) {
        return createResponse(ex, HttpStatus.BAD_REQUEST, "Некорректные параметры запроса: неверный формат ID");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(Exception ex) {
        return createResponse(ex, HttpStatus.BAD_REQUEST, "Некорректные параметры запроса: отсутствует заголовок");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(Exception ex) {
        return createResponse(ex, HttpStatus.BAD_REQUEST, "Некорректные параметры запроса: тело сообщения не читаемо");
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChatNotExists(Exception ex) {
        return createResponse(ex, HttpStatus.NOT_FOUND, "Чат не существует");
    }

    @ExceptionHandler(UnsupportedLinkException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedLink(Exception ex) {
        return createResponse(ex, HttpStatus.BAD_REQUEST, "Неподдерживаемая ссылка");
    }

    @ExceptionHandler(LinkAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkAlreadyExists(Exception ex) {
        return createResponse(ex, HttpStatus.CONFLICT, "Уже существующая ссылка");
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(Exception ex) {
        return createResponse(ex, HttpStatus.BAD_REQUEST, "Некоректные параметры запроса");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNoSuchElementException(Exception ex) {
        return createResponse(ex, HttpStatus.NOT_FOUND, "Данный элемент не найден");
    }

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
