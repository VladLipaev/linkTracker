package backend.academy.linktracker.scrapper.service;

public class LinkAlreadyExistsException extends RuntimeException {
    public LinkAlreadyExistsException(String message) {
        super(message);
    }
}
