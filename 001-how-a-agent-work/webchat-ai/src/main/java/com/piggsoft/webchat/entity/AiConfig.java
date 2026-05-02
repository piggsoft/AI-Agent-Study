package com.piggsoft.webchat.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;
}
