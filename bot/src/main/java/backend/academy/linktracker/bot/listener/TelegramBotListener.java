package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.handler.TelegramUpdateDispatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
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
                    for (Update update : updates) {
                        try {
                            dispatcher.dispatch(update);
                        } catch (Exception e) {
                            log.atError()
                                    .setMessage("Ошибка при обработке апдейта")
                                    .addKeyValue("update_id", update.updateId())
                                    .setCause(e)
                                    .log();
                        }
                    }
                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                },
                e -> {
                    if (e.response() != null) {
                        log.atError()
                                .setMessage("Ошибка Телеграм api в UpdatesListener")
                                .addKeyValue("telegram_error_code", e.response().errorCode())
                                .addKeyValue(
                                        "telegram_error_description",
                                        e.response().description())
                                .setCause(e)
                                .log();
                    } else {
                        log.atError()
                                .setMessage("Проблема соединения с телеграм")
                                .addKeyValue("error_details", e.getMessage())
                                .setCause(e)
                                .log();
                    }
                });
    }

    @PreDestroy
    public void stop() {
        log.info("Остановка слушателя телеграм...");

        try {
            telegramBot.removeGetUpdatesListener();
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("Не получилось остановить слушателя")
                    .addKeyValue("error.kind", "telegram_listener_shutdown_error")
                    .setCause(e)
                    .log();
        }

        try {
            telegramBot.shutdown();
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("неполучилось остановить Telegram bot")
                    .addKeyValue("error.kind", "telegram_shutdown_error")
                    .setCause(e)
                    .log();
        }
    }
}
