package com.example.capstone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class VectorStoreResetServiceTest {

    @Test
    void clear_deletesAllVectorStoreRowsAndReturnsDeletedCount() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update("DELETE FROM vector_store")).thenReturn(3);

        VectorStoreResetService service = new VectorStoreResetService(jdbcTemplate);

        assertThat(service.clear()).isEqualTo(3);
        verify(jdbcTemplate).update("DELETE FROM vector_store");
    }
}
