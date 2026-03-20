package backend.academy.linktracker.scrapper.repository.raw;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.repository.RawSqlException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.access-type", havingValue = "SQL")
@Slf4j
public class SqlLinksRepository implements LinksRepository {

    private final DataSource dataSource;

    private static final String SQL_FIND_ALL_CHAT_IDS_BY_URL = "SELECT c.id from chats c "
            + "join subscriptions s on c.id = s.chat_id " + "join links l on s.link_id = l.id where l.url = ?";

    private static final String SQL_FIND_ALL_CHAT_IDS_BY_URL_PAGING =
            SQL_FIND_ALL_CHAT_IDS_BY_URL + " LIMIT ? OFFSET ?";

    private static final String SQL_UPDATE_LAST_UPDATED = "UPDATE links SET updated_at = ? WHERE url = ?";

    private static final String SQL_FIND_BY_CHAT_ID_AND_URL =
            "SELECT l.id AS link_id, l.url, l.updated_at " + "FROM links l "
                    + "JOIN subscriptions s ON l.id = s.link_id "
                    + "WHERE s.chat_id = ? AND l.url = ?";

    private static final String SQL_FIND_BY_URL =
            "SELECT l.id AS link_id, l.url, l.updated_at, c.id AS chat_id " + "FROM links l "
                    + "LEFT JOIN subscriptions s ON l.id = s.link_id "
                    + "LEFT JOIN chats c ON s.chat_id = c.id "
                    + "WHERE l.url = ?";

    private static final String SQL_INSERT_LINK = "INSERT INTO links (url, updated_at) VALUES (?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT l.id AS link_id, l.url, l.updated_at, c.id AS chat_id " + "FROM links l "
                    + "LEFT JOIN subscriptions s ON l.id = s.link_id "
                    + "LEFT JOIN chats c ON s.chat_id = c.id "
                    + "WHERE l.id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT l.id AS link_id, l.url, l.updated_at, c.id AS chat_id " + "FROM links l "
                    + "LEFT JOIN subscriptions s ON l.id = s.link_id "
                    + "LEFT JOIN chats c ON s.chat_id = c.id "
                    + "ORDER BY l.id";

    private static final String SQL_DELETE_BY_ID = "DELETE FROM links WHERE id = ?";

    private static final String SQL_EXISTS_BY_ID = "SELECT COUNT(*) FROM links WHERE id = ?";

    private Link extractBasicLinkData(ResultSet rs) throws SQLException {
        Link link = new Link();
        link.setId(rs.getLong("link_id"));
        link.setUrl(rs.getString("url"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            // Правильное чтение Timestamp в OffsetDateTime без сдвига времени
            link.setLastUpdated(
                    updatedAt.toInstant().atOffset(OffsetDateTime.now().getOffset()));
        }
        return link;
    }

    private Chat extractChatFromResultSet(ResultSet rs) throws SQLException {
        long chatId = rs.getLong("chat_id");
        if (rs.wasNull()) {
            return null;
        }
        return new Chat(chatId);
    }

    @Override
    public List<Long> findAllChatIdsByUrl(String url) {
        List<Long> ids = new ArrayList<>();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL_CHAT_IDS_BY_URL)) {

            stmt.setString(1, url);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RawSqlException("не удалось найти все айди чатов", e);
        }
        return ids;
    }

    @Override
    public Slice<Long> findAllChatIdsByUrl(String url, Pageable pageable) {
        List<Long> ids = new ArrayList<>();
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL_CHAT_IDS_BY_URL_PAGING)) {

            stmt.setString(1, url);
            stmt.setInt(2, limit);
            stmt.setLong(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RawSqlException("не удалось найти айди чатов", e);
        }

        boolean hasNext = ids.size() > pageable.getPageSize();
        if (hasNext) {
            ids.removeLast();
        }
        return new SliceImpl<>(ids, pageable, hasNext);
    }

    @Override
    public void updateLastUpdatedByUrl(String url, OffsetDateTime lastUpdateFromApi) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_LAST_UPDATED)) {

            // Используем .toInstant(), чтобы сохранить абсолютное время (UTC), а не локальное
            stmt.setTimestamp(1, Timestamp.from(lastUpdateFromApi.toInstant()));
            stmt.setString(2, url);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                log.atWarn()
                        .setMessage("Строка не была обновлена. Ссылка не найдена в БД")
                        .addKeyValue("url", url)
                        .log();
            }

        } catch (SQLException e) {
            throw new RawSqlException("не удалось обновить время изменения ссылки", e);
        }
    }

    @Override
    public Optional<Link> findByChatIdAndUrl(Long chatId, String url) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_CHAT_ID_AND_URL)) {

            stmt.setLong(1, chatId);
            stmt.setString(2, url);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Link link = extractBasicLinkData(rs);
                    // Добавляем чат вручную, так как мы точно знаем, что он привязан
                    link.getChats().add(new Chat(chatId));
                    return Optional.of(link);
                }
            }
        } catch (SQLException e) {
            throw new RawSqlException("не удалось найти ссылку по чату и url", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Link> findByUrl(String url) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_URL)) {

            stmt.setString(1, url);

            return getLink(stmt);
        } catch (SQLException e) {
            throw new RawSqlException("не удалось найти ссылку по url", e);
        }
    }

    @NotNull
    private Optional<Link> getLink(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            Link link = null;

            while (rs.next()) {
                if (link == null) {
                    link = extractBasicLinkData(rs);
                }
                Chat chat = extractChatFromResultSet(rs);
                if (chat != null) {
                    link.getChats().add(chat);
                }
            }
            return Optional.ofNullable(link);
        }
    }

    @Override
    public Link save(Link entity) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_LINK, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, entity.getUrl());
            stmt.setTimestamp(
                    2,
                    entity.getLastUpdated() != null
                            ? Timestamp.from(entity.getLastUpdated().toInstant())
                            : Timestamp.from(java.time.Instant.now()));
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Создание ссылки неудачно, ID не был получен.");
                }
            }
        } catch (SQLException e) {
            throw new RawSqlException("не удалось сохранить ссылку", e);
        }
        return entity;
    }

    @Override
    public Optional<Link> findById(Long id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {

            stmt.setLong(1, id);

            return getLink(stmt);
        } catch (SQLException e) {
            throw new RawSqlException("не удалось найти ссылку по id", e);
        }
    }

    @Override
    public List<Link> findAll() {
        // LinkedHashMap склеивает дубликаты (одна ссылка -> несколько чатов) и сохраняет порядок ORDER BY
        Map<Long, Link> linkMap = new LinkedHashMap<>();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long linkId = rs.getLong("link_id");

                // Достаем ссылку из мапы. Если её еще нет - создаем из ResultSet и кладем в мапу
                Link link = linkMap.computeIfAbsent(linkId, k -> {
                    try {
                        return extractBasicLinkData(rs);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

                // Достаем привязанный чат и добавляем в список чатов этой ссылки
                Chat chat = extractChatFromResultSet(rs);
                if (chat != null) {
                    link.getChats().add(chat);
                }
            }
        } catch (SQLException | RuntimeException e) {
            throw new RawSqlException("не удалось найти ссылки", e);
        }
        return new ArrayList<>(linkMap.values());
    }

    @Override
    public void deleteById(Long id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_BY_ID)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RawSqlException("неудачно удалить ссылку по айди", e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RawSqlException("не удалось чекнуть есть ли ссылка по айди", e);
        }
        return false;
    }
}
