package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.entity.Link;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LinksRepository {

    java.util.List<Link> findAllByChatId(Long chatId);

    Link save(Link link);

    Optional<Link> findByChatIdAndUrl(Long chatId, String url);

    List<String> findAllUniqueUrls();

    Optional<Link> getAnyLinkByUrl(String url);

    List<Long> findAllChatIdsByUrl(String url);

    void updateLastUpdatedByUrl(String url, OffsetDateTime lastUpdateFromApi);

    List<Link> findAllByChatIdAndTag(Long chatId, String tag);

    Optional<Link> deleteByChatIdAndUrl(Long chatId, String link);
}
