package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {

    @Query("SELECT DISTINCT sub FROM Subscription sub join fetch sub.link l"
            + " join fetch sub.tags WHERE sub.chat.id = :chatId AND :tag MEMBER OF sub.tags")
    Slice<Subscription> findSubscriptionsByChatIdAndTag(
            @Param("chatId") Long chatId, @Param("tag") String tag, Pageable pageable);

    @Query("SELECT DISTINCT sub FROM Subscription sub join fetch sub.link l"
            + " join fetch sub.tags WHERE sub.chat.id = :chatId AND :tag MEMBER OF sub.tags")
    List<Subscription> findSubscriptionsByChatIdAndTag(@Param("chatId") Long chatId, @Param("tag") String tag);

    @Modifying
    void deleteBySubscriptionId(SubscriptionId id);

    boolean existsByLinkId(Long id);

    @Query(
            "SELECT s.tags FROM Subscription s WHERE s.subscriptionId.chatId = :chatId AND s.subscriptionId.linkId = :linkId")
    List<String> findTagsByChatIdAndLinkId(@Param("chatId") Long chatId, @Param("linkId") Long linkId);

    @Query(
            "SELECT s.tags FROM Subscription s WHERE s.subscriptionId.chatId = :chatId AND s.subscriptionId.linkId = :linkId")
    Slice<String> findTagsByChatIdAndLinkId(
            @Param("chatId") Long chatId, @Param("linkId") Long linkId, Pageable pageable);
}
