package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramUpdateService {
    private final TelegramClientFacade telegramClientFacade;

    public void postUpdate(LinkUpdate linkUpdate) {
        String message = "Обновления: " + "\n" + linkUpdate.description();
        for (Long chatId : linkUpdate.tgChatIds()) {
            telegramClientFacade.sendMessage(chatId, message);
        }
    }
}
