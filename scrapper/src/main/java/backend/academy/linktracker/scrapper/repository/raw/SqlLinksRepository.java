package backend.academy.linktracker.scrapper.repository.raw;

import static backend.academy.linktracker.scrapper.repository.raw.DataAccessExceptionHandler.handleDataAccessException;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.repository.LinksRepository;
import backend.academy.linktracker.scrapper.repository.RawSqlException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.access-type", havingValue = "SQL")
@Slf4j
public class SqlLinksRepository implements LinksRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL_FIND_ALL_CHAT_IDS_BY_URL = "SELECT c.id from chats c "
            + "join subscriptions s on c.id = s.chat_id " + "join links l on s.link_id = l.id where l.url = ?";

    private static final String SQL_FIND_ALL_CHAT_IDS_BY_URL_PAGING =
            SQL_FIND_ALL_CHAT_IDS_BY_URL + " LIMIT ? OFFSET ?";

    private static final String SQL_UPDATE_LAST_UPDATED = "UPDATE links SET updated_at = ? WHERE url = ?";

    private static final String SQL_FIND_LINKS_TO_CHECK = "SELECT id as link_id, url, updated_at, checked_at "
            + "FROM links ORDER BY checked_at  NULLS FIRST LIMIT ? OFFSET ?";

    private static final String SQL_FIND_BY_CHAT_ID_AND_URL =
            "SELECT l.id AS link_id, l.url, l.updated_at, l.checked_at, s.chat_id as chat_id " + "FROM links l "
                    + "JOIN subscriptions s ON l.id = s.link_id "
                    + "WHERE s.chat_id = ? AND l.url = ?";

    private static final String SQL_FIND_BY_URL =
            "SELECT l.id AS link_id, l.url, l.updated_at, l.checked_at, c.id AS chat_id " + "FROM links l "
                    + "LEFT JOIN subscriptions s ON l.id = s.link_id "
                    + "LEFT JOIN chats c ON s.chat_id = c.id "
                    + "WHERE l.url = ?";

    private static final String SQL_INSERT_LINK = "INSERT INTO links (url, updated_at) VALUES (?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT l.id AS link_id, l.url, l.updated_at, l.checked_at, c.id AS chat_id " + "FROM links l "
                    + "LEFT JOIN subscriptions s ON l.id = s.link_id "
                    + "LEFT JOIN chats c ON s.chat_id = c.id "
                    + "WHERE l.id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT l.id AS link_id, l.url, l.updated_at, l.checked_at, c.id AS chat_id " + "FROM links l "
                    + "LEFT JOIN subscriptions s ON l.id = s.link_id "
                    + "LEFT JOIN chats c ON s.chat_id = c.id "
                    + "ORDER BY l.id";

    private static final String SQL_FIND_ALL_PAGING = """
        WITH paginated_links AS (
            SELECT id, url, updated_at, checked_at
            FROM links
            ORDER BY id
            LIMIT ? OFFSET ?
        )
        SELECT l.id AS link_id, l.url, l.updated_at, l.checked_at, c.id AS chat_id
        FROM paginated_links l
        LEFT JOIN subscriptions s ON l.id = s.link_id
        LEFT JOIN chats c ON s.chat_id = c.id
        ORDER BY l.id
        """;

    private static final String SQL_DELETE_BY_ID = "DELETE FROM links WHERE id = ?";

    private static final String SQL_EXISTS_BY_ID = "SELECT EXISTS(SELECT 1 FROM links WHERE id = ?)";

    private final RowMapper<Link> linkRowMapper = (rs, rowNum) -> {
        Link link = new Link();
        link.setId(rs.getLong("link_id"));
        link.setUrl(rs.getString("url"));
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            link.setLastUpdated(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
        }
        Timestamp checkedAt = rs.getTimestamp("checked_at");
        if (checkedAt != null) {
            link.setLastCheckedAt(checkedAt.toInstant().atOffset(ZoneOffset.UTC));
        }
        return link;
    };

    private static Link extractData(ResultSet rs) throws SQLException {
        Link link = null;
        while (rs.next()) {
            if (link == null) {
                link = new Link();
                link.setId(rs.getLong("link_id"));
                link.setUrl(rs.getString("url"));
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    link.setLastUpdated(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
                }
                Timestamp checkedAt = rs.getTimestamp("checked_at");
                if (checkedAt != null) {
                    link.setLastUpdated(checkedAt.toInstant().atOffset(ZoneOffset.UTC));
                }
            }
            long chatId = rs.getLong("chat_id");
            if (!rs.wasNull()) {
                link.addChat(new Chat(chatId));
            }
        }
        return link;
    }

    @Override
    public List<Long> findAllChatIdsByUrl(String url) {
        try {
            return jdbcTemplate.queryForList(SQL_FIND_ALL_CHAT_IDS_BY_URL, Long.class, url);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public Slice<Long> findAllChatIdsByUrl(String url, Pageable pageable) {
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();
        try {
            List<Long> ids = new ArrayList<>(
                    jdbcTemplate.queryForList(SQL_FIND_ALL_CHAT_IDS_BY_URL_PAGING, Long.class, url, limit, offset));
            boolean hasNext = ids.size() > pageable.getPageSize();
            if (hasNext) {
                ids.removeLast();
            }
            return new SliceImpl<>(ids, pageable, hasNext);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public void updateLastUpdatedByUrl(String url, OffsetDateTime lastUpdateFromApi) {
        Timestamp ts = Timestamp.from(lastUpdateFromApi.toInstant());
        try {
            int rowsUpdated = jdbcTemplate.update(SQL_UPDATE_LAST_UPDATED, ts, url);
            if (rowsUpdated == 0) {
                log.atWarn()
                        .setMessage("Строка не была обновлена. Ссылка не найдена в БД")
                        .addKeyValue("url", url)
                        .log();
            }
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public Optional<Link> findByChatIdAndUrl(Long chatId, String url) {
        try {
            Link link = jdbcTemplate.query(SQL_FIND_BY_CHAT_ID_AND_URL, SqlLinksRepository::extractData, chatId, url);
            if (link != null) {
                link.addChat(new Chat(chatId));
            }
            return Optional.ofNullable(link);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public Optional<Link> findByUrl(String url) {
        try {
            Link result = jdbcTemplate.query(SQL_FIND_BY_URL, SqlLinksRepository::extractData, url);
            return Optional.ofNullable(result);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public void updateLastCheckedAt(List<Long> linkIds, OffsetDateTime checkedAt) {
        if (linkIds == null || linkIds.isEmpty()) return;

        String sql = "UPDATE links SET checked_at = ? WHERE id = ?";
        Timestamp ts = Timestamp.from(checkedAt.toInstant());
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NotNull PreparedStatement ps, int i) throws SQLException {
                ps.setTimestamp(1, ts);
                ps.setLong(2, linkIds.get(i));
            }

            @Override
            public int getBatchSize() {
                return linkIds.size();
            }
        });
    }

    @Override
    public Slice<Link> findLinksToCheck(Pageable pageable) {
        return findAllPaging(pageable, SqlLinksRepository::getLinks);
    }

    private @NotNull SliceImpl<Link> findAllPaging(
            Pageable pageable, ResultSetExtractor<List<Link>> resultSetExtractor) {
        int limit = pageable.getPageSize() + 1;
        long offset = pageable.getOffset();

        try {
            List<Link> links = jdbcTemplate.query(SQL_FIND_ALL_PAGING, resultSetExtractor, limit, offset);
            boolean hasNext = links.size() > pageable.getPageSize();
            if (hasNext) {
                links.removeLast();
            }
            return new SliceImpl<>(links, pageable, hasNext);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public Slice<Link> findAll(Pageable pageable) {
        return findAllPaging(pageable, SqlLinksRepository::getLinksWithChats);
    }

    @Override
    public Link save(Link entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(
                    connection -> {
                        PreparedStatement ps =
                                connection.prepareStatement(SQL_INSERT_LINK, Statement.RETURN_GENERATED_KEYS);
                        try {
                            ps.setString(1, entity.getUrl());
                            ps.setTimestamp(
                                    2, Timestamp.from(entity.getLastUpdated().toInstant()));
                            return ps;
                        } catch (SQLException | RuntimeException e) {
                            try {
                                ps.close();
                            } catch (SQLException ex) {
                                e.addSuppressed(ex);
                            }
                            throw e;
                        }
                    },
                    keyHolder);

            if (keyHolder.getKeys() != null) {
                Number key = (Number) keyHolder.getKeys().get("id");
                if (key != null) {
                    entity.setId(key.longValue());
                }
            }
            return entity;
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public Optional<Link> findById(Long id) {
        try {
            Link result = jdbcTemplate.query(SQL_FIND_BY_ID, SqlLinksRepository::extractData, id);

            return Optional.ofNullable(result);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    @Override
    public List<Link> findAll() {
        try {
            return jdbcTemplate.query(SQL_FIND_ALL, SqlLinksRepository::getLinksWithChats);
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }

    private static List<Link> getLinksWithChats(ResultSet rs) throws SQLException {
        Map<Long, Link> linkMap = new LinkedHashMap<>();
        while (rs.next()) {
            Link link = getLink(rs, linkMap);
            long chatId = rs.getLong("chat_id");
            if (!rs.wasNull()) {
                link.addChat(new Chat(chatId));
            }
        }
        return new ArrayList<>(linkMap.values());
    }

    private static List<Link> getLinks(ResultSet rs) throws SQLException {
        Map<Long, Link> linkMap = new LinkedHashMap<>();
        while (rs.next()) {
            getLink(rs, linkMap);
        }
        return new ArrayList<>(linkMap.values());
    }

    private static Link getLink(ResultSet rs, Map<Long, Link> linkMap) throws SQLException {
        Long linkId = rs.getLong("link_id");
        Link link;
        if (linkMap.containsKey(linkId)) {
            link = linkMap.get(linkId);
        } else {
            link = new Link();
            link.setId(linkId);
            link.setUrl(rs.getString("url"));
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                link.setLastUpdated(updatedAt.toInstant().atOffset(ZoneOffset.UTC));
            }
            Timestamp checkedAt = rs.getTimestamp("checked_at");
            if (checkedAt != null) {
                link.setLastCheckedAt(checkedAt.toInstant().atOffset(ZoneOffset.UTC));
            }
            linkMap.put(linkId, link);
        }
        return link;
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

    @Override
    public boolean existsById(Long id) {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(SQL_EXISTS_BY_ID, Boolean.class, id));
        } catch (DataAccessException e) {
            handleDataAccessException(e);
            throw new RawSqlException(e);
        }
    }
}
