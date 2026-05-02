package com.piggsoft.webchat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggsoft.webchat.dto.ApiResponse;
import com.piggsoft.webchat.dto.ChatHistoryDTO;
import com.piggsoft.webchat.dto.ChatRequest;
import com.piggsoft.webchat.entity.ChatHistory;
import com.piggsoft.webchat.mapper.ChatHistoryMapper;
import com.piggsoft.webchat.service.custom.CustomToolCallService;
import com.piggsoft.webchat.service.springai.SpringAiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SpringAiChatService springAiChatService;
    private final CustomToolCallService customToolCallService;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;

    /**
     * 方式一：Spring AI @Tool 注解方式
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        log.info("收到请求 [Spring AI @Tool]: sessionId={}", request.sessionId());
        return springAiChatService.chatStream(request)
                .map(ChatResponse::getResult)
                .filter(Objects::nonNull)
                .map(result -> Optional.ofNullable(result.getOutput().getText()).orElse(""))
                .filter(StringUtils::hasLength);
    }

    /**
     * 方式二：完全手写 WebClient 方式（手动构建 tool 定义、解析 tool_calls、执行工具、回传结果）
     */
    @PostMapping(value = "/stream/custom", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChatCustom(@RequestBody ChatRequest request) {
        log.info("收到请求 [Custom WebClient]: sessionId={}", request.sessionId());
        return customToolCallService.chatStream(request);
    }

    @GetMapping("/history/{sessionId}")
    public ApiResponse<List<ChatHistoryDTO>> getHistory(@PathVariable String sessionId) {
        List<ChatHistory> histories = chatHistoryMapper.selectBySessionId(sessionId);

        List<ChatHistoryDTO> dtos = histories.stream()
                .map(this::toDTO)
                .toList();

        return ApiResponse.success(dtos);
    }

    private ChatHistoryDTO toDTO(ChatHistory history) {
        Object queryConditions = null;
        if (history.getQueryConditions() != null) {
            try {
                queryConditions = objectMapper.readValue(history.getQueryConditions(), Object.class);
            } catch (JsonProcessingException e) {
                queryConditions = history.getQueryConditions();
            }
        }

        return new ChatHistoryDTO(
                history.getId(),
                history.getSessionId(),
                history.getRole(),
                history.getContent(),
                queryConditions,
                history.getCreatedAt()
        );
    }
}
