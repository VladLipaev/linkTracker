package backend.academy.linktracker.scrapper.repository.raw;

import static backend.academy.linktracker.scrapper.repository.raw.DataAccessExceptionHandler.handleDataAccessException;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.repository.RawSqlException;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.access-type", havingValue = "SQL")
@Slf4j
public class SqlTgChatRepository implements TgChatRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_SAVE = "INSERT INTO chats (id) VALUES (?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT c.id AS chat_id, l.id AS link_id, l.url, l.updated_at " + "FROM chats c "
                    + "LEFT JOIN subscriptions s ON c.id = s.chat_id "
                    + "LEFT JOIN links l ON s.link_id = l.id "
                    + "WHERE c.id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT c.id AS chat_id, l.id AS link_id, l.url, l.updated_at " + "FROM chats c "
                    + "LEFT JOIN subscriptions s ON c.id = s.chat_id "
                    + "LEFT JOIN links l ON s.link_id = l.id "
                    + "ORDER BY c.id";

    private static final String SQL_EXISTS_BY_ID = "SELECT COUNT(*) FROM chats WHERE id = ?";

    private static final String SQL_DELETE_BY_ID = "DELETE FROM chats WHERE id = ?";

    private static final String SQL_FIND_ALL_PAGED = """
            WITH paginated_chats AS (
                SELECT id
                FROM chats
                ORDER BY id
                LIMIT ? OFFSET ?
            )
            SELECT c.id AS chat_id, l.id AS link_id, l.url, l.updated_at
            FROM paginated_chats c
            LEFT JOIN subscriptions s ON c.id = s.chat_id
            LEFT JOIN links l ON s.link_id = l.id
            ORDER BY c.id
            """;

    private final ResultSetExtractor<List<Chat>> chatExtractor = rs -> {
        Map<Long, Chat> chatMap = new LinkedHashMap<>();

        while (rs.next()) {
            long chatId = rs.getLong("chat_id");

            Chat chat = chatMap.computeIfAbsent(chatId, Chat::new);

            long linkId = rs.getLong("link_id");
            if (!rs.wasNull()) {
                Link link = new Link();
                link.setId(linkId);
                link.setUrl(rs.getString("url"));

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    link.setLastUpdated(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
                }

                chat.addLink(link);
            }
        }
        return new ArrayList<>(chatMap.values());
    };

    @Override
    public Slice<Chat> findAll(Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();

        try {
            List<Chat> chats = jdbcTemplate.query(SQL_FIND_ALL_PAGED, chatExtractor, limit, offset);
            boolean hasNext = chats.size() > pageable.getPageSize();
            if (hasNext) {
                chats.removeLast();
            }
            return new SliceImpl<>(chats, pageable, hasNext);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public Chat save(Chat chat) {
        if (chat.getId() == null) {
            throw new IllegalArgumentException("chat id must be not null");
        }

        try {
            jdbcTemplate.update(SQL_SAVE, chat.getId());
        } catch (DuplicateKeyException e) {
            log.atWarn().setMessage("Чат с таким id уже существует").setCause(e).log();
            throw new RawSqlException("чат уже существует", e);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
        return chat;
    }

    @Override
    public Optional<Chat> findById(Long id) {
        try {
            List<Chat> chats = jdbcTemplate.query(SQL_FIND_BY_ID, chatExtractor, id);
            if (chats != null && !chats.isEmpty()) {
                return Optional.of(chats.getFirst());
            }
            return Optional.empty();
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public List<Chat> findAll() {
        try {
            List<Chat> chats = jdbcTemplate.query(SQL_FIND_ALL, chatExtractor);
            return chats != null ? chats : new ArrayList<>();
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        try {
            Integer count = jdbcTemplate.queryForObject(SQL_EXISTS_BY_ID, Integer.class, id);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            jdbcTemplate.update(SQL_DELETE_BY_ID, id);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }
}
