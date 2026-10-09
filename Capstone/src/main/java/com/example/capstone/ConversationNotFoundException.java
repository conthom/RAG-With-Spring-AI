package com.example.capstone;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation not found: " + conversationId);
    }
}