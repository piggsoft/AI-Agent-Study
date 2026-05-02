package com.piggsoft.webchat.dto;

import java.util.Map;

public record ChatRequest(
    String sessionId,
    String message,
    Map<String, Object> queryConditions
) {}
