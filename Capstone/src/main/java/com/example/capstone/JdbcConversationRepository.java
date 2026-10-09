package com.example.capstone;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcConversationRepository implements ConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(UUID conversationId) {
        jdbcTemplate.update("INSERT INTO conversations (id) VALUES (?)", conversationId);
    }

    @Override
    public boolean exists(UUID conversationId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM conversations WHERE id = ?",
                Integer.class,
                conversationId);
        return count != null && count > 0;
    }

    @Override
    public void appendMessage(UUID conversationId, String role, String content) {
        jdbcTemplate.update(
                "INSERT INTO conversation_messages (conversation_id, role, content) VALUES (?, ?, ?)",
                conversationId,
                role,
                content);
        jdbcTemplate.update(
                "UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                conversationId);
    }

    @Override
    public List<ConversationMessage> findRecentMessages(UUID conversationId, int limit) {
        return jdbcTemplate.query(
                "SELECT role, content, created_at FROM ("
                        + "SELECT id, role, content, created_at FROM conversation_messages "
                        + "WHERE conversation_id = ? ORDER BY created_at DESC, id DESC LIMIT ?"
                        + ") recent ORDER BY created_at ASC, id ASC",
                (resultSet, rowNumber) -> new ConversationMessage(
                        resultSet.getString("role"),
                        resultSet.getString("content"),
                        resultSet.getTimestamp("created_at").toInstant()),
                conversationId,
                limit);
    }

    @Override
    public boolean delete(UUID conversationId) {
        return jdbcTemplate.update("DELETE FROM conversations WHERE id = ?", conversationId) > 0;
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return jdbcTemplate.update(
                "DELETE FROM conversations WHERE updated_at < ?",
                java.sql.Timestamp.from(cutoff));
    }
}