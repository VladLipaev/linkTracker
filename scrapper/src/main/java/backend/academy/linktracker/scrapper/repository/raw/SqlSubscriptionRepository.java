package backend.academy.linktracker.scrapper.repository.raw;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.entity.Subscription;
import backend.academy.linktracker.scrapper.entity.SubscriptionId;
import backend.academy.linktracker.scrapper.repository.RawSqlException;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.access-type", havingValue = "SQL")
public class SqlSubscriptionRepository implements SubscriptionRepository {

    private final DataSource dataSource;

    // LEFT JOIN для маппинга тегов в коллекции
    private static final String SQL_FIND_BY_CHAT_AND_TAG =
            "SELECT s.chat_id, s.link_id, l.url, l.updated_at, t.tag " + "FROM subscriptions s "
                    + "JOIN links l ON s.link_id = l.id "
                    + "LEFT JOIN subscriptions_tags t ON s.chat_id = t.chat_id AND s.link_id = t.link_id "
                    + "WHERE s.chat_id = ? AND (? IS NULL OR t.tag = ?)";

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
            "SELECT s.chat_id, s.link_id, l.url, l.updated_at, t.tag " + "FROM subscriptions s "
                    + "JOIN links l ON s.link_id = l.id "
                    + "LEFT JOIN subscriptions_tags t ON s.chat_id = t.chat_id AND s.link_id = t.link_id "
                    + "WHERE s.chat_id = ? AND s.link_id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT s.chat_id, s.link_id, l.url, l.updated_at, t.tag " + "FROM subscriptions s "
                    + "JOIN links l ON s.link_id = l.id "
                    + "LEFT JOIN subscriptions_tags t ON s.chat_id = t.chat_id AND s.link_id = t.link_id "
                    + "ORDER BY s.chat_id, s.link_id";

    private static final String SQL_EXISTS_BY_ID =
            "SELECT COUNT(*) FROM subscriptions WHERE chat_id = ? AND link_id = ?";

    private List<Subscription> extractSubscriptionList(ResultSet rs) throws SQLException {
        Map<SubscriptionId, Subscription> map = new LinkedHashMap<>();

        while (rs.next()) {
            long chatId = rs.getLong("chat_id");
            long linkId = rs.getLong("link_id");
            SubscriptionId id = new SubscriptionId(chatId, linkId);

            // Создаем или получаем из мапы объект подписки
            Subscription sub = map.computeIfAbsent(id, k -> {
                Subscription s = new Subscription(chatId, linkId);
                s.setChat(new Chat(chatId)); // Заполняем привязанный чат

                Link link = new Link();
                link.setId(linkId);
                try {
                    link.setUrl(rs.getString("url"));
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        link.setLastUpdated(updatedAt
                                .toLocalDateTime()
                                .atOffset(OffsetDateTime.now().getOffset()));
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                s.setLink(link); // Заполняем привязанную ссылку
                s.setTags(new ArrayList<>());
                return s;
            });

            String tag = rs.getString("tag");
            if (tag != null && !sub.getTags().contains(tag)) {
                sub.getTags().add(tag);
            }
        }
        return new ArrayList<>(map.values());
    }

    @Override
    public Slice<Subscription> findSubscriptionsByChatIdAndTag(Long chatId, String tag, Pageable pageable) {
        List<Subscription> subs;
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_CHAT_AND_TAG_PAGED)) {

            stmt.setLong(1, chatId);
            stmt.setString(2, tag);
            stmt.setString(3, tag);
            stmt.setInt(4, limit);
            stmt.setLong(5, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                subs = extractSubscriptionList(rs);
            }
        } catch (SQLException | RuntimeException e) {
            log.error("Ошибка поиска подписок с тегом chatId={}, tag={}", chatId, tag, e);
            throw new RawSqlException("не удалось найти подписки по чату и тегу", e);
        }

        boolean hasNext = subs.size() > pageable.getPageSize();
        if (hasNext) {
            subs.removeLast();
        }
        return new SliceImpl<>(subs, pageable, hasNext);
    }

    @Override
    public List<Subscription> findSubscriptionsByChatIdAndTag(Long chatId, String tag) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_CHAT_AND_TAG)) {

            stmt.setLong(1, chatId);
            stmt.setString(2, tag);
            stmt.setString(3, tag);

            try (ResultSet rs = stmt.executeQuery()) {
                return extractSubscriptionList(rs);
            }
        } catch (SQLException | RuntimeException e) {
            log.error("Ошибка поиска списка подписок с тегом chatId={}, tag={}", chatId, tag, e);
            throw new RawSqlException("не удалось найти список подписок по чату и тегу", e);
        }
    }

    @Override
    public void deleteBySubscriptionId(SubscriptionId id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BY_ID)) {

            stmt.setLong(1, id.getChatId());
            stmt.setLong(2, id.getLinkId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error("Ошибка удаления подписки chatId={}, linkId={}", id.getChatId(), id.getLinkId(), e);
            throw new RawSqlException("не удалось удалить подписку", e);
        }
    }

    @Override
    public boolean existsByLinkId(Long linkId) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS_BY_LINK_ID)) {

            stmt.setLong(1, linkId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка проверки существования по linkId={}", linkId, e);
            throw new RawSqlException("не удалось проверить существование по linkId", e);
        }
        return false;
    }

    @Override
    public List<String> findTagsByChatIdAndLinkId(Long chatId, Long linkId) {
        List<String> tags = new ArrayList<>();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_TAGS)) {

            stmt.setLong(1, chatId);
            stmt.setLong(2, linkId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString("tag"));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка поиска тегов chatId={}, linkId={}", chatId, linkId, e);
            throw new RawSqlException("не удалось найти теги", e);
        }
        return tags;
    }

    @Override
    public Slice<String> findTagsByChatIdAndLinkId(Long chatId, Long linkId, Pageable pageable) {
        List<String> tags = new ArrayList<>();
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_TAGS_PAGED)) {

            stmt.setLong(1, chatId);
            stmt.setLong(2, linkId);
            stmt.setInt(3, limit);
            stmt.setLong(4, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString("tag"));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка поиска тегов с пагинацией chatId={}, linkId={}", chatId, linkId, e);
            throw new RawSqlException("не удалось найти теги с пагинацией", e);
        }

        boolean hasNext = tags.size() > pageable.getPageSize();
        if (hasNext) {
            tags.removeLast();
        }
        return new SliceImpl<>(tags, pageable, hasNext);
    }

    @Override
    public Subscription save(Subscription entity) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_SUBSCRIPTION)) {

            stmt.setLong(1, entity.getSubscriptionId().getChatId());
            stmt.setLong(2, entity.getSubscriptionId().getLinkId());
            stmt.executeUpdate();

            if (entity.getTags() != null && !entity.getTags().isEmpty()) {
                try (PreparedStatement tagStmt = conn.prepareStatement(SQL_INSERT_TAGS)) {
                    for (String tag : entity.getTags()) {
                        tagStmt.setLong(1, entity.getSubscriptionId().getChatId());
                        tagStmt.setLong(2, entity.getSubscriptionId().getLinkId());
                        tagStmt.setString(3, tag);
                        tagStmt.addBatch(); // Добавляем в batch
                    }
                    tagStmt.executeBatch(); // Выполняем пачкой
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().equals("23505")) {
                log.warn(
                        "Попытка сохранения дубликата подписки chatId={}, linkId={}",
                        entity.getSubscriptionId().getChatId(),
                        entity.getSubscriptionId().getLinkId());
            } else {
                log.error("Ошибка сохранения подписки", e);
            }
            throw new RawSqlException("не удалось сохранить подписку", e);
        }
        return entity;
    }

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            stmt.setLong(1, id.getChatId());
            stmt.setLong(2, id.getLinkId());

            try (ResultSet rs = stmt.executeQuery()) {
                List<Subscription> subs = extractSubscriptionList(rs);
                // Благодаря LEFT JOIN теги соберутся в extractSubscriptionList автоматически
                if (!subs.isEmpty()) {
                    return Optional.of(subs.getFirst());
                }
            }
        } catch (SQLException | RuntimeException e) {
            log.error("Ошибка поиска подписки по id chatId={}, linkId={}", id.getChatId(), id.getLinkId(), e);
            throw new RawSqlException("не удалось найти подписку по id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Subscription> findAll() {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
                ResultSet rs = stmt.executeQuery()) {

            return extractSubscriptionList(rs);

        } catch (SQLException | RuntimeException e) {
            log.error("Ошибка получения всех подписок", e);
            throw new RawSqlException("не удалось получить все подписки", e);
        }
    }

    @Override
    public void deleteById(SubscriptionId id) {
        deleteBySubscriptionId(id);
    }

    @Override
    public boolean existsById(SubscriptionId id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS_BY_ID)) {

            stmt.setLong(1, id.getChatId());
            stmt.setLong(2, id.getLinkId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка проверки существования подписки chatId={}, linkId={}", id.getChatId(), id.getLinkId(), e);
            throw new RawSqlException("не удалось проверить существование подписки", e);
        }
        return false;
    }
}
