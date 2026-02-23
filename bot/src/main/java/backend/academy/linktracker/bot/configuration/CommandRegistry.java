package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.handler.CommandHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeAllPrivateChats;
import com.pengrad.telegrambot.request.SetMyCommands;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bot.command-registry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommandRegistry {
    private final TelegramBot telegramBot;
    private final List<CommandHandler> commandHandlers;

    @EventListener(ContextRefreshedEvent.class)
    public void initCommands() {
        BotCommand[] botCommands = commandHandlers.stream()
                .filter(command -> !command.getCommandName().isBlank())
                .map(handler ->
                        new BotCommand(handler.getCommandName().substring(1).toLowerCase(), handler.getDescription()))
                .toArray(BotCommand[]::new);

        SetMyCommands request = new SetMyCommands(botCommands);
        request.scope(new BotCommandScopeAllPrivateChats());

        var response = telegramBot.execute(request);

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
