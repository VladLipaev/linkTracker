package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.entity.Link;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinksRepository implements LinksRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);

    private final List<Link> storage = new CopyOnWriteArrayList<>();

    @Override
    public List<Link> findAllByChatId(Long chatId) {
        return storage.stream().filter(l -> l.getChatId().equals(chatId)).toList();
    }

    @Override
    public Link save(Link link) {
        link.setId(idGenerator.incrementAndGet());
        storage.add(link);
        return link;
    }

    @Override
    public Optional<Link> findByChatIdAndUrl(Long chatId, String url) {
        return storage.stream()
                .filter(link -> Objects.equals(link.getUrl(), url) && Objects.equals(chatId, link.getChatId()))
                .findFirst();
    }

    @Override
    public List<String> findAllUniqueUrls() {
        return storage.stream().map(Link::getUrl).distinct().toList();
    }

    @Override
    public Optional<Link> getAnyLinkByUrl(String url) {
        return storage.stream()
                .filter(link -> Objects.equals(link.getUrl(), url))
                .findFirst();
    }

    @Override
    public List<Long> findAllChatIdsByUrl(String url) {
        return storage.stream()
                .filter(link -> Objects.equals(link.getUrl(), url))
                .map(Link::getChatId)
                .toList();
    }

    @Override
    public void updateLastUpdatedByUrl(String url, OffsetDateTime lastUpdateFromApi) {
        storage.stream()
                .filter(link -> Objects.equals(link.getUrl(), url))
                .forEach(link -> link.setLastUpdated(lastUpdateFromApi));
    }

    @Override
    public List<Link> findAllByChatIdAndTag(Long chatId, String tag) {
        return storage.stream()
                .filter(link -> Objects.equals(link.getChatId(), chatId)
                        && link.getTags().contains(tag))
                .toList();
    }

    @Override
    public Optional<Link> deleteByChatIdAndUrl(Long chatId, String url) {
        return storage.stream()
                .filter(link -> Objects.equals(link.getChatId(), chatId) && Objects.equals(link.getUrl(), url))
                .findFirst()
                .map(link -> {
                    storage.remove(link);
                    return link;
                });
    }
}
