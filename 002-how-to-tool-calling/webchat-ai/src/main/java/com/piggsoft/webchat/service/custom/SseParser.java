package com.piggsoft.webchat.service.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * SSE 数据行解析：提取 delta.content、类型安全转换。
 */
@Slf4j
public class SseParser {

    private final ObjectMapper objectMapper;

    public SseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 从 SSE 的 data 行 JSON 中提取 choices[0].delta.content
     */
    public String extractDeltaContent(String jsonData) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                log.warn("[Custom] SSE数据缺少choices: {}", jsonData);
                return "";
            }

            JsonNode delta = choices.get(0).get("delta");
            if (delta == null) {
                log.warn("[Custom] SSE数据缺少delta: {}", jsonData);
                return "";
            }

            if (delta.has("tool_calls")) {
                log.info("[Custom] SSE包含tool_calls (非文本内容): {}", delta.get("tool_calls"));
                return "";
            }

            JsonNode content = delta.get("content");
            if (content != null && !content.asText().isEmpty()) {
                //log.debug("[Custom] 提取内容: {}", content.asText());
                return content.asText();
            }
            return "";
        } catch (Exception e) {
            log.warn("[Custom] SSE行解析失败: {}", jsonData);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> toList(Object obj) {
        return obj instanceof List ? (List<Map<String, Object>>) obj : null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : null;
    }
}
