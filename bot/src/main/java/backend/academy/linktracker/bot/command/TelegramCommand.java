package backend.academy.linktracker.bot.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TelegramCommand {
    START("/start", "Приветствие юзера", true),
    HELP("/help", "список доступных команд", true);

    private final String value;
    private final String description;
    private final boolean enabled;
}
