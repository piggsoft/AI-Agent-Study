package com.piggsoft.webchat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggsoft.webchat.entity.AiConfig;
import com.piggsoft.webchat.entity.Order;
import com.piggsoft.webchat.mapper.AiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private final AiConfigMapper aiConfigMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构建 System Message（静态的角色定义和规则）
     */
    public SystemMessage buildSystemMessage() {
        String systemPrompt = getSystemPrompt();

        String prompt = """
            %s
            
            【回答要求】
            1. 基于业务数据回答用户问题
            2. 如果数据不足或无法回答，请明确告知
            3. 回答要简洁、准确
            """.formatted(systemPrompt);

        return new SystemMessage(prompt);
    }

    /**
     * 构建 User Message（包含业务数据、查询条件和用户问题）
     */
    public UserMessage buildUserMessage(String userMessage, Map<String, Object> queryConditions, List<Order> businessData) {
        // 1. 使用 JDK 17 文本块定义清晰的模板
        String template = """
            {businessSection}
            {querySection}
            【用户问题】
            {userMessage}
            """;

        // 2. 准备数据
        Map<String, Object> model = new HashMap<>();
        model.put("userMessage", userMessage);

        // 条件渲染数据块
        model.put("businessSection", businessData != null && !businessData.isEmpty() ?
                "【业务数据】\n" + toJsonSafe(businessData) + "\n" : "");

        model.put("querySection", queryConditions != null && !queryConditions.isEmpty() ?
                "【查询条件】\n" + toJsonSafe(queryConditions) + "\n" : "");

        // 3. 依靠 Spring AI 自动渲染并生成 Message
        return (UserMessage) PromptTemplate.builder().template(template).variables(model).build().createMessage();
    }

    // 抽取出来的私有方法，保证核心逻辑的干净
    private String toJsonSafe(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize data type: {}", data.getClass().getSimpleName(), e);
            return data instanceof List ? "[]" : "{}"; // 提供更安全的 fallback
        }
    }

    private String getSystemPrompt() {
        return aiConfigMapper.selectByConfigKey("system_prompt")
                .map(AiConfig::getConfigValue)
                .orElse("你是一个数据分析助手，可以根据提供的业务数据回答用户问题。");
    }
}
