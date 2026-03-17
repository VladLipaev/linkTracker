package backend.academy.linktracker.scrapper.config;

import backend.academy.linktracker.scrapper.service.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.service.ChatNotFoundException;
import backend.academy.linktracker.scrapper.service.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.service.UnsupportedLinkException;
import io.grpc.Status;
import io.grpc.StatusException;
import java.util.NoSuchElementException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

@Configuration
public class GrpcExceptionConfig {
    @Bean
    public GrpcExceptionHandler grpcExceptionHandler() {
        return ex -> switch (ex) {
            case ChatAlreadyExistsException e ->
                new StatusException(Status.ALREADY_EXISTS.withDescription("Чат уже существует: " + e.getMessage()));

            case ChatNotFoundException e ->
                new StatusException(Status.NOT_FOUND.withDescription("Чат не существует: " + e.getMessage()));

            case UnsupportedLinkException e -> new StatusException(
                Status.INVALID_ARGUMENT.withDescription("Неподдерживаемая ссылка: " + e.getMessage()));

            case LinkAlreadyExistsException e -> new StatusException(
                Status.ALREADY_EXISTS.withDescription("Уже существующая ссылка: " + e.getMessage()));

            case NoSuchElementException e ->
                new StatusException(Status.NOT_FOUND.withDescription("Данный элемент не найден: " + e.getMessage()));
            default -> new StatusException(Status.INTERNAL
                .withDescription("Внутренняя ошибка сервера: " + ex.getMessage())
                .withCause(ex));
        };
    }
}
