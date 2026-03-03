package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.handler.TelegramUpdateDispatcher;
import backend.academy.linktracker.bot.listener.TelegramBotListener;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramBotStart {

    private final CommandRegistry commandRegistry;
    private final TelegramUpdateDispatcher telegramUpdateDispatcher;
    private final TelegramBotListener telegramBotListener;

    @EventListener(ContextRefreshedEvent.class)
    public void start() {
        log.info("Starting up telegram bot");
        telegramBotListener.start();
        telegramUpdateDispatcher.initHandlerMap();
        commandRegistry.initCommands();
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down telegram bot");
        telegramBotListener.stop();
    }
}
