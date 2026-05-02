package com.piggsoft.webchat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggsoft.webchat.dto.ChatRequest;
import com.piggsoft.webchat.entity.ChatHistory;
import com.piggsoft.webchat.mapper.ChatHistoryMapper;
import com.piggsoft.webchat.utils.PrettyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final PromptBuilder promptBuilder;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;
    private final Advisor loggerAdvisor = new PrettyLoggerAdvisor();

    public ChatServiceImpl(PromptBuilder promptBuilder, BusinessDataService businessDataService,
                           ChatHistoryMapper chatHistoryMapper, ObjectMapper objectMapper,
                           ChatClient.Builder builder, org.springframework.ai.chat.memory.ChatMemory chatMemory,
                           ToolCallingManager toolCallingManager) {
        this.promptBuilder = promptBuilder;
        this.chatHistoryMapper = chatHistoryMapper;
        this.objectMapper = objectMapper;

        this.chatClient = builder
                .defaultTools(businessDataService)
                .defaultAdvisors(loggerAdvisor, MessageChatMemoryAdvisor.builder(chatMemory).build(), ToolCallAdvisor.builder().toolCallingManager(toolCallingManager).streamToolCallResponses(true).build())
                .build();
    }

    @Override
    public Flux<ChatResponse> chatStream(ChatRequest request) {
        log.info("Processing chat request: sessionId={}, message={}",
                request.sessionId(), request.message());

        Flux<ChatResponse> dbSearchMessage = Flux.concat(
                Flux.just(new ChatResponse(List.of(
                        new Generation(new AssistantMessage("正在为您检索数据库相关信息...\n"))
                )))
        );

        // 2. 保存用户消息
        chatHistoryMapper.insert(buildChatHistory(
                request.sessionId(),
                "user",
                request.message(),
                request.queryConditions()
        ));

        // 3. 构建消息列表
        List<Message> messages = List.of(
                promptBuilder.buildSystemMessage(),
                new UserMessage(request.message())
        );

        // 4. 调用流式 API
        return Flux.concat(
                dbSearchMessage,
                chatClient.prompt().messages(messages).stream().chatResponse()
        );
    }

    private ChatHistory buildChatHistory(String sessionId, String role, String content, Object queryConditions) {
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
