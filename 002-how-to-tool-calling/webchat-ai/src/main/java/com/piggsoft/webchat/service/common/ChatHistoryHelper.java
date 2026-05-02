package com.piggsoft.webchat.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggsoft.webchat.entity.ChatHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryHelper {

    private final ObjectMapper objectMapper;

    public ChatHistory buildChatHistory(String sessionId, String role, String content, Object queryConditions) {
        String queryConditionsJson = null;
        if (queryConditions != null) {
            try {
                queryConditionsJson = objectMapper.writeValueAsString(queryConditions);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize query conditions", e);
            }
        }
        return new ChatHistory(null, sessionId, role, content, queryConditionsJson, null);
    }
}
