package backend.academy.linktracker.bot.client.scrapper;

import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import java.util.List;

public interface ScrapperClient {
    void registerChat(long chatId);

    LinkResponse addLink(long chatId, String link, List<String> tags);

    ListLinksResponse getLinks(long chatId, String tag);

    LinkResponse removeLink(long chatId, String text);
}
