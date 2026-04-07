package backend.academy.linktracker.scrapper.repository;

public class RawSqlException extends RuntimeException {
    public RawSqlException(String message) {
        super(message);
    }

    public RawSqlException(String message, Throwable cause) {
        super(message, cause);
    }

    public RawSqlException(Throwable cause) {
        super("ошибка со стороны бд", cause);
    }
}
