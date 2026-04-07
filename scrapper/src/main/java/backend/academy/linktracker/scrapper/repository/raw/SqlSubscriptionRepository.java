package backend.academy.linktracker.scrapper.repository.raw;

import static backend.academy.linktracker.scrapper.repository.raw.DataAccessExceptionHandler.handleDataAccessException;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import backend.academy.linktracker.scrapper.repository.RawSqlException;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.access-type", havingValue = "SQL")
public class SqlSubscriptionRepository implements SubscriptionRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_FIND_BY_CHAT_AND_TAG =
            "SELECT s.chat_id, s.link_id, l.url, l.updated_at, t.tag FROM subscriptions s "
                    + "JOIN links l ON s.link_id = l.id "
                    + "LEFT JOIN subscriptions_tags t ON s.chat_id = t.chat_id AND s.link_id = t.link_id "
                    + "WHERE s.chat_id = ? AND (? IS NULL OR t.tag = ?)";
    private static final String SQL_FIND_ALL_PAGED = """
        WITH paginated_subs AS (
            SELECT chat_id, link_id
            FROM subscriptions
            ORDER BY chat_id, link_id
            LIMIT ? OFFSET ?
        )
        SELECT s.chat_id, s.link_id, l.url, l.updated_at, t.tag
        FROM paginated_subs s
        JOIN links l ON s.link_id = l.id
        LEFT JOIN subscriptions_tags t ON s.chat_id = t.chat_id AND s.link_id = t.link_id
        ORDER BY s.chat_id, s.link_id
        """;
    private static final String SQL_FIND_BY_CHAT_AND_TAG_PAGED = SQL_FIND_BY_CHAT_AND_TAG + " LIMIT ? OFFSET ?";
    private static final String SQL_DELETE_BY_ID = "DELETE FROM subscriptions WHERE chat_id = ? AND link_id = ?";
    private static final String SQL_EXISTS_BY_LINK_ID = "SELECT COUNT(*) FROM subscriptions WHERE link_id = ?";
    private static final String SQL_FIND_TAGS =
            "SELECT tag FROM subscriptions_tags WHERE chat_id = ? AND link_id = ? ORDER BY tag";
    private static final String SQL_FIND_TAGS_PAGED = SQL_FIND_TAGS + " LIMIT ? OFFSET ?";
    private static final String SQL_INSERT_SUBSCRIPTION = "INSERT INTO subscriptions (chat_id, link_id) VALUES (?, ?)";
    private static final String SQL_INSERT_TAGS =
            "INSERT INTO subscriptions_tags (chat_id, link_id, tag) VALUES (?, ?, ?)";
    private static final String SQL_FIND_BY_ID =
            "SELECT s.chat_id, s.link_id, l.url, l.updated_at, t.tag FROM subscriptions s "
                    + "JOIN links l ON s.link_id = l.id "
                    + "LEFT JOIN subscriptions_tags t ON s.chat_id = t.chat_id AND s.link_id = t.link_id "
                    + "WHERE s.chat_id = ? AND s.link_id = ?";
    private static final String SQL_FIND_ALL =
            "SELECT s.chat_id, s.link_id, l.url, l.updated_at, t.tag FROM subscriptions s "
                    + "JOIN links l ON s.link_id = l.id "
                    + "LEFT JOIN subscriptions_tags t ON s.chat_id = t.chat_id AND s.link_id = t.link_id "
                    + "ORDER BY s.chat_id, s.link_id";
    private static final String SQL_EXISTS_BY_ID =
            "SELECT COUNT(*) FROM subscriptions WHERE chat_id = ? AND link_id = ?";

    private final ResultSetExtractor<List<Subscription>> subscriptionExtractor = rs -> {
        Map<SubscriptionId, Subscription> map = new LinkedHashMap<>();

        while (rs.next()) {
            long chatId = rs.getLong("chat_id");
            long linkId = rs.getLong("link_id");
            SubscriptionId id = new SubscriptionId(chatId, linkId);

            Subscription sub = map.computeIfAbsent(id, k -> {
                Subscription s = new Subscription(chatId, linkId);
                s.setChat(new Chat(chatId));

                Link link = new Link();
                link.setId(linkId);
                try {
                    link.setUrl(rs.getString("url"));
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        link.setLastUpdated(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                s.setLink(link);
                s.setTags(new ArrayList<>());
                return s;
            });

            String tag = rs.getString("tag");
            if (tag != null && !sub.getTags().contains(tag)) {
                sub.getTags().add(tag);
            }
        }
        return new ArrayList<>(map.values());
    };

    @Override
    public Slice<Subscription> findSubscriptionsByChatIdAndTag(Long chatId, String tag, Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();

        try {
            List<Subscription> subs = jdbcTemplate.query(
                    SQL_FIND_BY_CHAT_AND_TAG_PAGED, subscriptionExtractor, chatId, tag, tag, limit, offset);

            return getSubscriptions(pageable, subs);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public List<Subscription> findSubscriptionsByChatIdAndTag(Long chatId, String tag) {
        try {
            List<Subscription> subs =
                    jdbcTemplate.query(SQL_FIND_BY_CHAT_AND_TAG, subscriptionExtractor, chatId, tag, tag);
            return subs != null ? subs : new ArrayList<>();
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public void deleteBySubscriptionId(SubscriptionId id) {
        try {
            jdbcTemplate.update(SQL_DELETE_BY_ID, id.getChatId(), id.getLinkId());
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public boolean existsByLinkId(Long linkId) {
        try {
            Integer count = jdbcTemplate.queryForObject(SQL_EXISTS_BY_LINK_ID, Integer.class, linkId);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public List<String> findTagsByChatIdAndLinkId(Long chatId, Long linkId) {
        try {
            return jdbcTemplate.queryForList(SQL_FIND_TAGS, String.class, chatId, linkId);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public Slice<String> findTagsByChatIdAndLinkId(Long chatId, Long linkId, Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();
        try {
            List<String> tags = new ArrayList<>(
                    jdbcTemplate.queryForList(SQL_FIND_TAGS_PAGED, String.class, chatId, linkId, limit, offset));

            boolean hasNext = tags.size() > pageable.getPageSize();
            if (hasNext) {
                tags.removeLast();
            }
            return new SliceImpl<>(tags, pageable, hasNext);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public Slice<Subscription> findAll(Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();
        try {
            List<Subscription> subs = jdbcTemplate.query(SQL_FIND_ALL_PAGED, subscriptionExtractor, limit, offset);

            return getSubscriptions(pageable, subs);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @NotNull
    private Slice<Subscription> getSubscriptions(Pageable pageable, List<Subscription> subs) {
        boolean hasNext = subs.size() > pageable.getPageSize();
        if (hasNext) {
            subs.removeLast();
        }
        return new SliceImpl<>(subs, pageable, hasNext);
    }

    @Override
    public Subscription save(Subscription entity) {
        try {
            jdbcTemplate.update(
                    SQL_INSERT_SUBSCRIPTION,
                    entity.getSubscriptionId().getChatId(),
                    entity.getSubscriptionId().getLinkId());

            if (entity.getTags() != null && !entity.getTags().isEmpty()) {
                jdbcTemplate.batchUpdate(SQL_INSERT_TAGS, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, entity.getSubscriptionId().getChatId());
                        ps.setLong(2, entity.getSubscriptionId().getLinkId());
                        ps.setString(3, entity.getTags().get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return entity.getTags().size();
                    }
                });
            }
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
        return entity;
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        try {
            List<Subscription> subs =
                    jdbcTemplate.query(SQL_FIND_BY_ID, subscriptionExtractor, id.getChatId(), id.getLinkId());
            if (subs != null && !subs.isEmpty()) {
                return Optional.of(subs.getFirst());
            }
            return Optional.empty();
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public List<Subscription> findAll() {
        try {
            List<Subscription> subs = jdbcTemplate.query(SQL_FIND_ALL, subscriptionExtractor);
            return subs != null ? subs : new ArrayList<>();
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public void deleteById(SubscriptionId id) {
        deleteBySubscriptionId(id);
    }

    @Override
    public boolean existsById(SubscriptionId id) {
        try {
            Integer count =
                    jdbcTemplate.queryForObject(SQL_EXISTS_BY_ID, Integer.class, id.getChatId(), id.getLinkId());
            return count != null && count > 0;
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }
}
