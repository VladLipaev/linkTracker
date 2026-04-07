package backend.academy.linktracker.scrapper.repository.raw;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;

@Slf4j
public class DataAccessExceptionHandler {

    public static void handleDataAccessException(DataAccessException e) {
        log.atError()
                .setMessage("Ошибка на стороне бд")
                .addKeyValue("error.message", e.getMessage())
                .setCause(e)
                .log();
    }
}
