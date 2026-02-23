package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.handler.TelegramUpdateDispatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBotListener {

    private final TelegramBot telegramBot;
    private final TelegramUpdateDispatcher dispatcher;

    @PostConstruct
    public void start() {
        telegramBot.setUpdatesListener(
                updates -> {
                    updates.forEach(dispatcher::dispatch);
                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                },
                e -> {
                    try (var _ = MDC.putCloseable("event.type", "network_event");
                            var _ = MDC.putCloseable("event.status", "failure");
                            var _ = MDC.putCloseable(
                                    "error.kind", e.response() != null ? "telegram_api" : "network_connectivity")) {

                        if (e.response() != null) {
                            log.atError()
                                    .setMessage("Telegram API Error in UpdatesListener")
                                    .addKeyValue(
                                            "telegram_error_code", e.response().errorCode())
                                    .addKeyValue(
                                            "telegram_error_description",
                                            e.response().description())
                                    .setCause(e)
                                    .log();
                        } else {
                            log.atError()
                                    .setMessage("Telegram Network Connectivity Issue")
                                    .addKeyValue("error_details", e.getMessage())
                                    .setCause(e)
                                    .log();
                        }
                    }
                });
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down Telegram Bot listener...");
    }
}
