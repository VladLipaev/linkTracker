package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends BaseRepository<Subscription, SubscriptionId> {

    Slice<Subscription> findSubscriptionsByChatIdAndTag(Long chatId,String tag, Pageable pageable);

    List<Subscription> findSubscriptionsByChatIdAndTag(Long chatId, String tag);

    void deleteBySubscriptionId(SubscriptionId id);

    boolean existsByLinkId(Long linkId);

    List<String> findTagsByChatIdAndLinkId(Long chatId, Long linkId);

    Slice<String> findTagsByChatIdAndLinkId(Long chatId, Long linkId, Pageable pageable);
}
