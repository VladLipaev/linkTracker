package backend.academy.linktracker.scrapper.repository;

public class RawSqlException extends RuntimeException {
    public RawSqlException(String message) {
        super(message);
    }

    public RawSqlException(String message, Throwable cause) {
        super(message, cause);
    }
}
