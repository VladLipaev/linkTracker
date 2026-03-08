package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.handler.CommandHandler;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.response.BaseResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandRegistry {
    private final TelegramClientFacade facade;
    private final List<CommandHandler> commandHandlers;

    public void initCommands() {
        BotCommand[] botCommands = commandHandlers.stream()
                .filter(command -> !command.getCommandName().isBlank() && command.isEnabled())
                .map(handler ->
                        new BotCommand(handler.getCommandName().substring(1).toLowerCase(), handler.getDescription()))
                .toArray(BotCommand[]::new);

        BaseResponse response = facade.setBotCommands(botCommands);

        if (!response.isOk()) {
            log.atError()
                    .setMessage("Failed to register bot commands")
                    .addKeyValue("error.kind", "telegram_api_error")
                    .addKeyValue("telegram_error_code", response.errorCode())
                    .addKeyValue("telegram_error_description", response.description())
                    .log();
        } else {
            log.atInfo()
                    .setMessage("Successfully registered commands")
                    .addKeyValue("commands_count", botCommands.length)
                    .log();
        }
    }
}
