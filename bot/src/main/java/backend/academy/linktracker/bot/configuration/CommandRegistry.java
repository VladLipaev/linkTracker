package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.handler.CommandHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandRegistry {
    private final TelegramBot telegramBot;
    private final List<CommandHandler> commandHandlers;

    @EventListener(ContextRefreshedEvent.class)
    public void initCommands() {
        BotCommand[] botCommands = commandHandlers.stream()
                .map(handler -> new BotCommand(handler.getCommandName().substring(1), handler.getDescription()))
                .toArray(BotCommand[]::new);
        telegramBot.execute(new SetMyCommands(botCommands));
    }
}
