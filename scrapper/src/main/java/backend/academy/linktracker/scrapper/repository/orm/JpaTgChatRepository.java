package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.dto.ChatSummary;
import backend.academy.linktracker.scrapper.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface JpaTgChatRepository extends JpaRepository<Chat, Long> {

    @Query("select c from Chat c where c.id = :id")
    @EntityGraph(value = "Chat.withLinks", type = EntityGraph.EntityGraphType.FETCH)
    Optional<Chat> findChatWithLinks(Long id);


    @Query("select new backend.academy.linktracker.scrapper.dto.ChatSummary(c.id, count(s)) FROM Chat c left join c.subscriptions s group by c.id")
    Page<ChatSummary> findAllAndSubsSize(Pageable pageable);
}
