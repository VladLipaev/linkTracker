package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.configuration.TelegramUpdateDispatcher;
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
                            log.error(
                                    "Telegram API Error: {} - {}",
                                    e.response().errorCode(),
                                    e.response().description());
                        } else {
                            log.error("Network connectivity issue: {}", e.getMessage());
                        }
                    }
                });
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down Telegram Bot listener...");
        telegramBot.removeGetUpdatesListener();
    }
}
