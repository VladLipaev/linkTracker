package backend.academy.linktracker.ai.client;

public class YandexGPTApiException extends RuntimeException {
    public YandexGPTApiException(String message) {
        super(message);
    }

    public YandexGPTApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
