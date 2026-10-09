package com.example.capstone;

import java.time.Instant;

public record ConversationMessage(String role, String content, Instant createdAt) {
}