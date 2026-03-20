package backend.academy.linktracker.scrapper.repository.raw;

import backend.academy.linktracker.scrapper.entity.Chat;
import backend.academy.linktracker.scrapper.entity.Link;
import backend.academy.linktracker.scrapper.repository.RawSqlException;
import backend.academy.linktracker.scrapper.repository.TgChatRepository;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.db.access-type", havingValue = "SQL")
public class SqlTgChatRepository implements TgChatRepository {

    private final DataSource dataSource;

    private static final String SQL_SAVE = "INSERT INTO chats (id) VALUES (?);";

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

    private static final String SQL_EXISTS_BY_ID = "SELECT COUNT(*) FROM chats WHERE id = ?;";
    private static final String SQL_DELETE_BY_ID = "DELETE FROM chats WHERE id = ?;";

    private Link extractLinkFromResultSet(ResultSet rs) throws SQLException {
        long linkId = rs.getLong("link_id");
        if (rs.wasNull()) {
            return null; // У чата нет ссылок
        }

        Link link = new Link();
        link.setId(linkId);
        link.setUrl(rs.getString("url"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            link.setLastUpdated(
                    updatedAt.toLocalDateTime().atOffset(OffsetDateTime.now().getOffset()));
        }

        return link;
    }

    @Override
    public Chat save(Chat chat) {
        if (chat.getId() == null) {
            throw new IllegalArgumentException("chat id must be not null");
        }
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(SQL_SAVE)) {

            ps.setLong(1, chat.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RawSqlException("не удалось вставить чат", e);
        }
        return chat;
    }

    @Override
    public Optional<Chat> findById(Long id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                Chat chat = null;

                // проходимся по всем строкам
                while (rs.next()) {
                    if (chat == null) {
                        // Создаем чат на первой итерации. (Коллекция links инициализируется пустой в конструкторе)
                        chat = new Chat(rs.getLong("chat_id"));
                    }

                    // Пробуем извлечь ссылку из текущей строки
                    Link link = extractLinkFromResultSet(rs);
                    if (link != null) {
                        chat.getLinks().add(link);
                    }
                }
                return Optional.ofNullable(chat);
            }
        } catch (SQLException e) {
            throw new RawSqlException("не удалось найти чат по айди", e);
        }
    }

    @Override
    public List<Chat> findAll() {
        // Используем LinkedHashMap, чтобы сохранить сортировку "ORDER BY c.id" и склеивать дублирующиеся id чатов
        Map<Long, Chat> chatMap = new LinkedHashMap<>();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement stmt = conn.prepareStatement(SQL_FIND_ALL);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long chatId = rs.getLong("chat_id");

                // Достаем чат из мапы, если его еще нет - создаем
                Chat chat = chatMap.computeIfAbsent(chatId, Chat::new);

                // Добавляем ссылку (если она есть в этой строке)
                Link link = extractLinkFromResultSet(rs);
                if (link != null) {
                    chat.getLinks().add(link);
                }
            }

        } catch (SQLException e) {
            throw new RawSqlException("не удалось найти все чаты", e);
        }

        return new ArrayList<>(chatMap.values());
    }

    @Override
    public boolean existsById(Long id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_BY_ID)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;

        } catch (SQLException e) {
            throw new RawSqlException("не удалось найти по id", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_BY_ID)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RawSqlException("не удалось удалить чат", e);
        }
    }
}
