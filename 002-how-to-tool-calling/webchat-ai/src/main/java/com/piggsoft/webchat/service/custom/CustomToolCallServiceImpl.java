package com.piggsoft.webchat.service.custom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggsoft.webchat.dto.ChatRequest;
import com.piggsoft.webchat.mapper.ChatHistoryMapper;
import com.piggsoft.webchat.service.common.ChatHistoryHelper;
import com.piggsoft.webchat.service.common.PromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 完全手写 WebClient 的 Tool Calling 实现。
 * 不使用 Spring AI 的 @Tool 注解，而是手动构建 tool 定义、解析 LLM 返回的 tool_calls、
 * 本地执行函数后把结果回传给 LLM，最终流式返回答案。
 *
 * 核心流转：非流式调用 LLM → 判断 finish_reason → 本地执行工具 → 流式返回最终答案
 */
@Service
@Slf4j
public class CustomToolCallServiceImpl implements CustomToolCallService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.completions-path:/v1/chat/completions}")
    private String completionsPath;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final PromptBuilder promptBuilder;
    private final ChatHistoryHelper chatHistoryHelper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ToolDefinitionBuilder toolDefinitionBuilder;
    private final ToolExecutor toolExecutor;
    private final SseParser sseParser;

    public CustomToolCallServiceImpl(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                                     PromptBuilder promptBuilder, ChatHistoryHelper chatHistoryHelper,
                                     ChatHistoryMapper chatHistoryMapper,
                                     ToolDefinitionBuilder toolDefinitionBuilder, ToolExecutor toolExecutor) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.chatHistoryHelper = chatHistoryHelper;
        this.chatHistoryMapper = chatHistoryMapper;
        this.toolDefinitionBuilder = toolDefinitionBuilder;
        this.toolExecutor = toolExecutor;
        this.sseParser = new SseParser(objectMapper);
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        log.info("Processing chat request [Custom WebClient]: sessionId={}, message={}",
                request.sessionId(), request.message());

        chatHistoryMapper.insert(chatHistoryHelper.buildChatHistory(
                request.sessionId(), "user", request.message(), request.queryConditions()));

        return doChatStream(request);
    }

    /* ======================== 核心流程（两阶段） ======================== */

    private Flux<String> doChatStream(ChatRequest request) {
        String systemPrompt = promptBuilder.buildSystemPromptText();

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", request.message()));

        List<Map<String, Object>> tools = toolDefinitionBuilder.buildToolDefinitions();

        return callNonStreaming(messages, tools)
                .flatMapMany(response -> {
                    List<Map<String, Object>> choices = SseParser.toList(response.get("choices"));
                    if (choices == null || choices.isEmpty()) {
                        log.error("[Custom] 模型返回的 choices 为空! 完整响应: {}", toJson(response));
                        return Flux.just("[错误] 模型未返回有效响应");
                    }
                    Map<String, Object> choice = choices.get(0);
                    String finishReason = (String) choice.get("finish_reason");
                    Map<String, Object> assistantMsg = SseParser.toMap(choice.get("message"));

                    if ("tool_calls".equals(finishReason) && assistantMsg != null) {
                        String assistantContent = assistantMsg.get("content") instanceof String s ? s : "";
                        assistantContent = !assistantContent.isBlank() ? assistantContent : "";

                        List<Map<String, Object>> toolCalls = SseParser.toList(assistantMsg.get("tool_calls"));
                        messages.add(assistantMsg);

                        if (toolCalls != null) {
                            for (Map<String, Object> tc : toolCalls) {
                                Map<String, Object> function = SseParser.toMap(tc.get("function"));
                                if (function == null) continue;

                                String toolName = (String) function.get("name");
                                String arguments = (String) function.get("arguments");
                                String toolCallId = (String) tc.get("id");

                                log.info("[Custom] LLM 请求调用工具: {} 参数: {}", toolName, arguments);
                                String toolResult = toolExecutor.executeTool(toolName, arguments);
                                log.info("[Custom] 工具返回: {}", toolResult);

                                messages.add(Map.of(
                                        "role", "tool",
                                        "tool_call_id", toolCallId != null ? toolCallId : "unknown",
                                        "content", toolResult
                                ));
                            }
                        }

                        log.info("[Custom] 第二轮消息 (含工具结果): {}", toJson(messages));
                        if (!assistantContent.isEmpty()) {
                            log.info("[Custom] 先发送 assistant 文本: {}", assistantContent);
                            return Flux.concat(
                                    Flux.just(assistantContent),
                                    callStreaming(messages, tools)
                            );
                        }
                        return callStreaming(messages, tools);
                    }

                    log.info("[Custom] 无 tool call，finishReason={}, assistant内容={}",
                            finishReason,
                            assistantMsg != null ? assistantMsg.get("content") : "无");
                    if (assistantMsg != null) {
                        messages.add(assistantMsg);
                    }
                    return callStreaming(messages, tools);
                })
                .onErrorResume(e -> {
                    log.error("[Custom] doChatStream 异常", e);
                    return Flux.just("\n[系统异常: " + e.getMessage() + "]");
                });
    }

    /* ======================== LLM 调用 ======================== */

    private Mono<Map<String, Object>> callNonStreaming(List<Map<String, Object>> messages,
                                                        List<Map<String, Object>> tools) {
        Map<String, Object> body = buildRequestBody(messages, tools, false);
        log.info("[Custom] 发起非流式请求 (判断是否需要 tool call)");
        log.info("[Custom] 非流式请求体: {}", toJson(body));

        return webClient.post()
                .uri(baseUrl + completionsPath)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnNext(resp -> log.info("[Custom] 非流式响应 (finish_reason={}): {}",
                        extractFinishReason(resp), toJson(resp)));
    }

    private Flux<String> callStreaming(List<Map<String, Object>> messages,
                                        List<Map<String, Object>> tools) {
        Map<String, Object> body = buildRequestBody(messages, tools, true);
        log.info("[Custom] 发起流式请求 (获取最终答案)");
        log.info("[Custom] 流式请求体: {}", toJson(body));

        // 用 StringBuilder 收集完整响应，流结束时打印
        StringBuilder fullContent = new StringBuilder();

        return webClient.post()
                .uri(baseUrl + completionsPath)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .exchangeToFlux(response -> {
                    if (!response.statusCode().is2xxSuccessful()) {
                        return response.createException()
                                .flatMapMany(ex -> Flux.<String>error(ex));
                    }
                    log.info("[Custom] 流式请求 HTTP 状态: {}", response.statusCode().value());
                    return response.bodyToFlux(String.class);
                })
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n")))
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !"[DONE]".equals(line))
                .map(line -> line.startsWith("data:") ? line.substring(5).strip() : line)
                .map(sseParser::extractDeltaContent)
                .filter(StringUtils::hasLength)
                .doOnNext(content -> {
                    fullContent.append(content);
                    //log.debug("[Custom] 流式内容片段: {}", content);
                })
                .doOnComplete(() -> log.info("[Custom] 流式响应完整内容: {}", fullContent))
                .onErrorResume(e -> {
                    log.error("[Custom] 流式请求异常", e);
                    return Flux.just("\n[流式响应异常: " + e.getMessage() + "]");
                });
    }

    /* ======================== 请求构建 ======================== */

    private Map<String, Object> buildRequestBody(List<Map<String, Object>> messages,
                                                  List<Map<String, Object>> tools,
                                                  boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("temperature", 0.7);
        body.put("top_p", 0.8);
        body.put("max_tokens", 4096);
        body.put("stream", stream);
        return body;
    }

    /* ======================== 响应解析 ======================== */

    private String extractFinishReason(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                return (String) choices.get(0).get("finish_reason");
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    /* ======================== 通用辅助 ======================== */

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"序列化失败: " + e.getMessage() + "\"}";
        }
    }
}
