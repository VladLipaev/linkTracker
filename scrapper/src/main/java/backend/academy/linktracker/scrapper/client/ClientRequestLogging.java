package backend.academy.linktracker.scrapper.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientRequestLogging {

    private ClientRequestLogging() {
    }

    public static void handleRequestFailure(String message, String eventType, String eventStatus, Throwable e) {
        log.atError()
            .setMessage(message)
            .addKeyValue("event.type", eventType)
            .addKeyValue("event.status", eventStatus)
            .setCause(e)
            .log();
    }

    public static void handleRequestSuccess(String message, String eventType, String eventStatus) {
        log.atInfo()
            .setMessage(message)
            .addKeyValue("event.type", eventType)
            .addKeyValue("event.status", eventStatus)
            .log();
    }
}
