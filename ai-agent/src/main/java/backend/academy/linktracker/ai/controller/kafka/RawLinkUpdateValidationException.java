package backend.academy.linktracker.ai.controller.kafka;

public class RawLinkUpdateValidationException extends RuntimeException {
    public RawLinkUpdateValidationException(String message) {
        super(message);
    }

    public RawLinkUpdateValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RawLinkUpdateValidationException(Throwable cause) {
        super(cause);
    }
}
