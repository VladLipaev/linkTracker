package backend.academy.linktracker.bot.handler.logging;

import backend.academy.linktracker.bot.handler.CommandHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TelegramDispatcherLogging {

    private TelegramDispatcherLogging() {}

    public static void ReceivedUpdate(Integer updateId, Long chatId, String text) {
        log.atDebug()
                .setMessage("Received update")
                .addKeyValue("event.type", "telegram_update")
                .addKeyValue("update_id", updateId)
                .addKeyValue("chat_id", chatId)
                .addKeyValue("has_text", text != null)
                .log();
    }

    public static void UnknownCommandHandled(Integer updateId, Long chatId, String text, long startTime) {

        log.atInfo()
                .setMessage("Unknown command handled")
                .addKeyValue("event.type", "unknown_command")
                .addKeyValue("event.status", "success")
                .addKeyValue("update_id", updateId)
                .addKeyValue("chat_id", chatId)
                .addKeyValue("raw_text_length", text.length())
                .addKeyValue("duration_ms", (System.nanoTime() - startTime) / 1_000_000.0)
                .log();
    }

    public static void CommandHandled(Integer updateId, Long chatId, long startTime, CommandHandler handler) {
        log.atInfo()
                .setMessage("Command handled")
                .addKeyValue("event.status", "success")
                .addKeyValue("command.name", handler.getCommandName())
                .addKeyValue("command.handler", handler.getClass().getSimpleName())
                .addKeyValue("update_id", updateId)
                .addKeyValue("chat_id", chatId)
                .addKeyValue("duration_ms", (System.nanoTime() - startTime) / 1_000_000.0)
                .log();
    }

    public static void CommandHandlingFailed(Integer updateId, Long chatId, String text, long startTime, Exception e) {
        log.atError()
                .setMessage("Command handling failed")
                .addKeyValue("event.status", "failure")
                .addKeyValue("error.kind", "command_execution_error")
                .addKeyValue("update_id", updateId)
                .addKeyValue("chat_id", chatId)
                .addKeyValue("duration_ms", (System.nanoTime() - startTime) / 1_000_000.0)
                .setCause(e)
                .log();
    }
}
