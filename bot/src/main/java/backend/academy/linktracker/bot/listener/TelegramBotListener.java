package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.handler.TelegramUpdateDispatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBotListener {

    private final TelegramBot telegramBot;
    private final TelegramUpdateDispatcher dispatcher;


    public void start() {
        telegramBot.setUpdatesListener(
                updates -> {
                    updates.forEach(dispatcher::dispatch);
                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                },
                e -> {
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
                        }});
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down Telegram Bot listener...");

        try {
            telegramBot.removeGetUpdatesListener();
            log.debug("Updates listener removed");
        } catch (Exception e) {
            log.atWarn()
                .setMessage("Failed to remove updates listener")
                .addKeyValue("error.kind", "telegram_listener_shutdown_error")
                .setCause(e)
                .log();
        }

        try {
            telegramBot.shutdown();
            log.debug("Telegram bot shutdown completed");
        } catch (Exception e) {
            log.atWarn()
                .setMessage("Failed to shutdown Telegram bot")
                .addKeyValue("error.kind", "telegram_shutdown_error")
                .setCause(e)
                .log();
        }
    }
}
