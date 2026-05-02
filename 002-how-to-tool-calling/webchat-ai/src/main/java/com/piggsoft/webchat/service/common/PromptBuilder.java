package com.piggsoft.webchat.service.common;

import com.piggsoft.webchat.entity.AiConfig;
import com.piggsoft.webchat.mapper.AiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private final AiConfigMapper aiConfigMapper;

    public String buildSystemPromptText() {
        String systemPrompt = getSystemPrompt();
        return """
            %s

            【回答要求】
            1. 基于业务数据回答用户问题
            2. 如果数据不足或无法回答，请明确告知
            3. 回答要简洁、准确
            """.formatted(systemPrompt);
    }

    private String getSystemPrompt() {
        return aiConfigMapper.selectByConfigKey("system_prompt")
                .map(AiConfig::getConfigValue)
                .orElse("你是一个数据分析助手，可以根据提供的业务数据回答用户问题。");
    }
}
