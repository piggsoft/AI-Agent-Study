package com.piggsoft.webchat.service.springai;

import com.piggsoft.webchat.dto.ChatRequest;
import com.piggsoft.webchat.mapper.ChatHistoryMapper;
import com.piggsoft.webchat.service.common.ChatHistoryHelper;
import com.piggsoft.webchat.service.common.PromptBuilder;
import com.piggsoft.webchat.utils.PrettyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Spring AI @Tool 注解方式的 Tool Calling 实现
 */
@Service
@Slf4j
public class SpringAiChatServiceImpl implements SpringAiChatService {

    private final PromptBuilder promptBuilder;
    private final ChatHistoryHelper chatHistoryHelper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatClient chatClient;
    private final Advisor loggerAdvisor = new PrettyLoggerAdvisor();

    public SpringAiChatServiceImpl(PromptBuilder promptBuilder, ToolService toolService,
                                   ChatHistoryMapper chatHistoryMapper, ChatHistoryHelper chatHistoryHelper,
                                   ChatClient.Builder builder, org.springframework.ai.chat.memory.ChatMemory chatMemory,
                                   ToolCallingManager toolCallingManager) {
        this.promptBuilder = promptBuilder;
        this.chatHistoryHelper = chatHistoryHelper;
        this.chatHistoryMapper = chatHistoryMapper;

        this.chatClient = builder
                .defaultTools(toolService)
                .defaultAdvisors(loggerAdvisor, MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        ToolCallAdvisor.builder().toolCallingManager(toolCallingManager).streamToolCallResponses(true).build())
                .build();
    }

    @Override
    public Flux<ChatResponse> chatStream(ChatRequest request) {
        log.info("Processing chat request [Spring AI @Tool]: sessionId={}, message={}",
                request.sessionId(), request.message());

        chatHistoryMapper.insert(chatHistoryHelper.buildChatHistory(
                request.sessionId(),
                "user",
                request.message(),
                request.queryConditions()
        ));

        List<Message> messages = List.of(
                new SystemMessage(promptBuilder.buildSystemPromptText()),
                new UserMessage(request.message())
        );

        return chatClient.prompt().messages(messages).stream().chatResponse();
    }
}
