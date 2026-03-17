package backend.academy.linktracker.bot.client.scrapper;

public class ScrapperClientException extends RuntimeException {

    public ScrapperClientException() {}

    public ScrapperClientException(String message) {
        super(message);
    }

    public ScrapperClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScrapperClientException(Throwable cause) {
        super(cause);
    }

    public ScrapperClientException(
            String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
