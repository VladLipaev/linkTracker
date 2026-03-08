package backend.academy.linktracker.scrapper.service;

public class ChatAlreadyExistsException extends RuntimeException {
    public ChatAlreadyExistsException(String message) {
        super(message);
    }
}
