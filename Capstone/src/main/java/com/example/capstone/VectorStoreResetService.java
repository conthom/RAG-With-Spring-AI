package com.example.capstone;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class VectorStoreResetService {

    private final JdbcTemplate jdbcTemplate;

    public VectorStoreResetService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int clear() {
        return jdbcTemplate.update("DELETE FROM vector_store");
    }
}
