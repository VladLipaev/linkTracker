package backend.academy.linktracker.bot.handler.dialog;

import backend.academy.linktracker.bot.client.scrapper.ScrapperClientException;
import backend.academy.linktracker.bot.client.scrapper.ScrapperRestClient;
import backend.academy.linktracker.bot.client.telegram.TelegramClientFacade;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
@RequiredArgsConstructor
@Slf4j
public class DialogStepProcessor {
    private final DialogManager dialogManager;
    private final TelegramClientFacade telegramClientFacade;
    private final ScrapperRestClient scrapperClient;
    private final BotLinkValidator linkValidator;

    public void process(Update update, UserSession session) {
        long chatId = update.message().chat().id();
        String text = update.message().text();

        try {
            switch (session.state()) {
                // состояние ожидания ссылки для добавления
                case WAITING_FOR_TRACK_LINK -> {
                    if (linkValidator.isValid(text)) {
                        // Ссылка валидна, идем дальше
                        dialogManager.setSession(chatId, new UserSession(UserState.WAITING_FOR_TRACK_TAGS, text));
                        telegramClientFacade.sendMessage(
                                chatId, "Ссылка принята. Теперь введите теги через запятую или напишите skip");
                    } else {
                        telegramClientFacade.sendMessage(
                                chatId,
                                "Некорректная ссылка! \nЯ поддерживаю только GitHub (репозитории) и StackOverflow (вопросы). \nПопробуйте еще раз или введите /cancel.");
                    }
                }
                // находится в состоянии ожидания ссылки чтобы ее удалить
                case WAITING_FOR_UNTRACK_LINK -> {
                    if (linkValidator.isValid(text)) {
                        LinkResponse linkResponse = scrapperClient.removeLink(chatId, text);
                        telegramClientFacade.sendMessage(
                                chatId, "Готово! Ссылка %s удалена из отслеживания.".formatted(linkResponse.url()));
                        dialogManager.setSession(chatId, UserSession.base());
                    } else {
                        telegramClientFacade.sendMessage(
                                chatId,
                                "Некорректная ссылка! \nЯ поддерживаю только GitHub (репозитории) и StackOverflow (вопросы). \nПопробуйте еще раз или введите /cancel.");
                    }
                }
                // находится в состоянии ожидания тегов
                case WAITING_FOR_TRACK_TAGS -> {
                    String link = session.tempLink();
                    List<String> tags = (text.equalsIgnoreCase("skip") || text.isBlank())
                            ? List.of()
                            : List.of(text.split("\\s*,\\s*"));

                    LinkResponse linkResponse = scrapperClient.addLink(chatId, link, tags);
                    telegramClientFacade.sendMessage(
                            chatId, "Готово! Ссылка %s добавлена в отслеживание.".formatted(linkResponse.url()));
                    dialogManager.setSession(chatId, UserSession.base());
                }

                // состояние ожидания тегов для нахождения списка ссылок по данному тегу
                case WAITING_FOR_LIST_TAG -> {
                    String tag = text.equalsIgnoreCase("skip") ? null : text;
                    ListLinksResponse response = scrapperClient.getLinks(chatId, tag);

                    if (response.links().isEmpty()) {
                        telegramClientFacade.sendMessage(
                                chatId, "У вас нет отслеживаемых ссылок" + (tag != null ? " с тегом " + tag : ""));
                    } else {
                        StringBuilder sb = new StringBuilder("Ваши ссылки:\n");
                        response.links().forEach(link -> sb.append(link.url()).append("\n"));
                        telegramClientFacade.sendMessage(chatId, sb.toString());
                    }
                    dialogManager.setSession(chatId, UserSession.base());
                }

                default -> dialogManager.setSession(chatId, UserSession.base());
            }
        } catch (ScrapperClientException e) {
            log.atError()
                    .setMessage("Ошибка: " + e.getMessage())
                    .addKeyValue("event.status", "failure")
                    .addKeyValue("error.kind", "scrapper_client_error")
                    .addKeyValue("chat_id", chatId)
                    .setCause(e)
                    .log();
            telegramClientFacade.sendMessage(chatId, "Ошибка: " + e.getMessage());

        } catch (ResourceAccessException e) {
            log.atError()
                    .setMessage("Скраппер недоступен")
                    .addKeyValue("event.status", "failure")
                    .addKeyValue("error.kind", "scrapper_execution_error")
                    .addKeyValue("chat_id", chatId)
                    .setCause(e)
                    .log();
            telegramClientFacade.sendMessage(chatId, "Сервис отслеживания временно недоступен. Попробуйте позже.");
        }
    }
}
