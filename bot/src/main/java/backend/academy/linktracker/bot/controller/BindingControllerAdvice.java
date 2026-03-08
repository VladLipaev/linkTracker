package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BindingControllerAdvice {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException e) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "Error while validating...",
                        "400",
                        e.getObjectName(),
                        e.getMessage(),
                        new ArrayList<>(Arrays.stream(e.getStackTrace())
                                .map(StackTraceElement::toString)
                                .toList())));
    }
}
