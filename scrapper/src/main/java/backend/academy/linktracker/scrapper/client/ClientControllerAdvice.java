package backend.academy.linktracker.scrapper.client;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;

@ControllerAdvice
public class ClientControllerAdvice {

    @ExceptionHandler
    public void handleRestClientException(RestClientException e) {
        ClientRequestLogging.handleRequestFailure("Неудачный запрос в SO", "stack_overflow_request", "failure", e);
        throw new StackOverflowException("StackOverflow API error: " + e.getMessage());
    }
}
