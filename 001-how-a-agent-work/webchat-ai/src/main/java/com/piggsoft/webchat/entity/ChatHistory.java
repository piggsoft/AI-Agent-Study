package com.piggsoft.webchat.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistory {
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String queryConditions;
    private LocalDateTime createdAt;
}
