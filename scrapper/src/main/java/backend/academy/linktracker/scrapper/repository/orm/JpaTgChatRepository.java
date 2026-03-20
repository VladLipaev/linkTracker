package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTgChatRepository extends JpaRepository<Chat, Long> {}
