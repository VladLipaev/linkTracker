package backend.academy.linktracker.bot.handler;

import com.pengrad.telegrambot.model.Update;

public interface CommandHandler {

    boolean canHandle(String text);

    void handle(Update update);

    String getCommandName();

    String getDescription();
}
