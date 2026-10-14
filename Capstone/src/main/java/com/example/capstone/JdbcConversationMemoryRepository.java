package com.example.capstone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcConversationMemoryRepository implements ConversationMemoryRepository {

    private static final String SELECT_COLUMNS = "id, conversation_id, content, source_content, status, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public JdbcConversationMemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(UUID id, UUID conversationId, String content, String sourceContent) {
        jdbcTemplate.update(
                "INSERT INTO conversation_memories (id, conversation_id, content, source_content, status) "
                        + "VALUES (?, ?, ?, ?, 'PENDING')",
                id,
                conversationId,
                content,
                sourceContent);
    }

    @Override
    public List<ConversationMemory> findByConversation(UUID conversationId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM conversation_memories "
                        + "WHERE conversation_id = ? ORDER BY created_at DESC, id DESC",
                this::mapMemory,
                conversationId);
    }

    @Override
    public Optional<ConversationMemory> findById(UUID conversationId, UUID id) {
        List<ConversationMemory> memories = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM conversation_memories WHERE conversation_id = ? AND id = ?",
                this::mapMemory,
                conversationId,
                id);
        return memories.stream().findFirst();
    }

    @Override
    public List<String> findApprovedContent(UUID conversationId) {
        return jdbcTemplate.queryForList(
                "SELECT content FROM conversation_memories "
                        + "WHERE conversation_id = ? AND status = 'APPROVED' ORDER BY updated_at DESC",
                String.class,
                conversationId);
    }

    @Override
    public boolean updatePending(UUID conversationId, UUID id, String content) {
        return jdbcTemplate.update(
                "UPDATE conversation_memories SET content = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE conversation_id = ? AND id = ? AND status = 'PENDING'",
                content,
                conversationId,
                id) == 1;
    }

    @Override
    public boolean transition(UUID conversationId, UUID id, MemoryStatus from, MemoryStatus to) {
        return jdbcTemplate.update(
                "UPDATE conversation_memories SET status = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE conversation_id = ? AND id = ? AND status = ?",
                to.name(),
                conversationId,
                id,
                from.name()) == 1;
    }

    @Override
    public boolean delete(UUID conversationId, UUID id) {
        return jdbcTemplate.update(
                "DELETE FROM conversation_memories WHERE conversation_id = ? AND id = ?",
                conversationId,
                id) == 1;
    }

    private ConversationMemory mapMemory(java.sql.ResultSet resultSet, int rowNumber)
            throws java.sql.SQLException {
        return new ConversationMemory(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("conversation_id", UUID.class),
                resultSet.getString("content"),
                resultSet.getString("source_content"),
                MemoryStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}