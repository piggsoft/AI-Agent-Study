package com.piggsoft.webchat.dto;

import java.time.LocalDateTime;

public record ChatHistoryDTO(
    Long id,
    String sessionId,
    String role,
    String content,
    Object queryConditions,
    LocalDateTime createdAt
) {}
