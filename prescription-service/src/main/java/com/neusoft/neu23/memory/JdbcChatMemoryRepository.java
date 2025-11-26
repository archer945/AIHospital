package com.neusoft.neu23.memory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * JDBC implementation of {@link ChatMemoryRepository} backed by spring_ai_chat_memory.
 * Persists message text and type for reuse across requests.
 */
@Component
public class JdbcChatMemoryRepository implements ChatMemoryRepository {

    private static final String TABLE = "spring_ai_chat_memory";

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> findConversationIds() {
        return jdbcTemplate.query("select distinct conversation_id from " + TABLE,
                (rs, idx) -> rs.getString(1));
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String sql = "select type, content from " + TABLE + " where conversation_id = ? order by timestamp asc";
        return jdbcTemplate.query(sql, new Object[]{conversationId}, new MessageRowMapper());
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // Preserve existing timestamps to avoid resetting all history to "now" on each save
        List<LocalDateTime> existingTimestamps = jdbcTemplate.query(
                "select timestamp from " + TABLE + " where conversation_id = ? order by timestamp asc",
                new Object[]{conversationId},
                (rs, idx) -> {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    return ts != null ? ts.toLocalDateTime() : null;
                }
        );

        jdbcTemplate.update("delete from " + TABLE + " where conversation_id = ?", conversationId);
        if (messages == null || messages.isEmpty()) {
            return;
        }
        String insertSql = "insert into " + TABLE + " (conversation_id, content, type, timestamp) values (?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        int idx = 0;
        List<Object[]> batch = new ArrayList<>();
        for (Message message : messages) {
            MessageType type = message.getMessageType();
            String content = Objects.toString(message.getText(), "");
            LocalDateTime ts = idx < existingTimestamps.size()
                    ? existingTimestamps.get(idx)
                    : now.plusNanos(idx);
            if (ts == null) {
                ts = now.plusNanos(idx);
            }
            idx++;
            batch.add(new Object[]{conversationId, content, type.getValue(), ts});
        }
        jdbcTemplate.batchUpdate(insertSql, batch);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        jdbcTemplate.update("delete from " + TABLE + " where conversation_id = ?", conversationId);
    }

    private static final class MessageRowMapper implements RowMapper<Message> {
        @Override
        public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
            String type = rs.getString("type");
            String content = rs.getString("content");
            String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "user" -> new UserMessage(content);
                case "assistant", "assistant_error" -> new AssistantMessage(content, Map.of());
                case "system" -> new SystemMessage(content);
                default -> new SystemMessage("[memory:" + normalized + "] " + content);
            };
        }
    }
}
