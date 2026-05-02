package com.piggsoft.webchat.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 格式化日志 Advisor，用于分行打印 LLM 的请求与响应细节，包含 Tool Call 全链路日志。
 */
public class PrettyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(PrettyLoggerAdvisor.class);
    private static final String NEW_LINE = "\n";

    public static final Function<ChatClientRequest, String> DEFAULT_REQUEST_TO_STRING = request -> {

        StringBuilder sb = new StringBuilder();
        sb.append(NEW_LINE).append("=".repeat(20)).append(" LLM REQUEST ").append("=".repeat(20)).append(NEW_LINE);

        String collect = request.prompt().getInstructions().stream()
                .map(msg -> {
                    StringBuilder msgSb = new StringBuilder();
                    msgSb.append("【").append(msg.getMessageType()).append("】: ");
                    if (msg instanceof ToolResponseMessage toolMsg) {
                        msgSb.append(NEW_LINE);
                        for (var resp : toolMsg.getResponses()) {
                            msgSb.append("  ┌─ ToolResponse").append(NEW_LINE);
                            msgSb.append("  │  name: ").append(resp.name()).append(NEW_LINE);
                            msgSb.append("  │  responseData: ").append(resp.responseData()).append(NEW_LINE);
                            msgSb.append("  └─").append(NEW_LINE);
                        }
                    } else if (msg instanceof AssistantMessage assistantMsg
                            && assistantMsg.getToolCalls() != null
                            && !assistantMsg.getToolCalls().isEmpty()) {
                        msgSb.append(NEW_LINE);
                        for (var toolCall : assistantMsg.getToolCalls()) {
                            msgSb.append("  ┌─ ToolCall (模型请求调用)").append(NEW_LINE);
                            msgSb.append("  │  id: ").append(toolCall.id()).append(NEW_LINE);
                            msgSb.append("  │  name: ").append(toolCall.name()).append(NEW_LINE);
                            msgSb.append("  │  arguments: ").append(toolCall.arguments()).append(NEW_LINE);
                            msgSb.append("  └─").append(NEW_LINE);
                        }
                        if (assistantMsg.getText() != null && !assistantMsg.getText().isEmpty()) {
                            msgSb.append("  文本: ").append(assistantMsg.getText());
                        }
                    } else {
                        msgSb.append(NEW_LINE).append(msg.getText());
                    }
                    return msgSb.toString();
                })
                .collect(Collectors.joining(NEW_LINE));

        sb.append(collect);

        if (request.prompt().getOptions() != null) {
            sb.append(NEW_LINE).append("【Options】: ").append(NEW_LINE).append(ModelOptionsUtils.toJsonString(request.prompt().getOptions())).append(NEW_LINE);
        }

        sb.append("=".repeat(53));
        return sb.toString();
    };

    public static final Function<ChatResponse, String> DEFAULT_RESPONSE_TO_STRING = response -> {
        StringBuilder sb = new StringBuilder();
        sb.append(NEW_LINE).append("=".repeat(20)).append(" LLM RESPONSE ").append("=".repeat(20)).append(NEW_LINE);

        if (response.getResult() != null && response.getResult().getOutput() != null) {
            var output = response.getResult().getOutput();

            if (output.getText() != null && !output.getText().isEmpty()) {
                sb.append("【文本输出】: ").append(output.getText()).append(NEW_LINE);
            }

            var toolCalls = output.getToolCalls();
            if (toolCalls != null && !toolCalls.isEmpty()) {
                sb.append(NEW_LINE).append("-".repeat(20)).append(" TOOL CALLS (模型请求调用工具) ").append("-".repeat(10)).append(NEW_LINE);
                for (var toolCall : toolCalls) {
                    sb.append("  ┌─ ToolCall").append(NEW_LINE);
                    sb.append("  │  id: ").append(toolCall.id()).append(NEW_LINE);
                    sb.append("  │  type: ").append(toolCall.type()).append(NEW_LINE);
                    sb.append("  │  name: ").append(toolCall.name()).append(NEW_LINE);
                    sb.append("  │  arguments: ").append(toolCall.arguments()).append(NEW_LINE);
                    sb.append("  └─").append(NEW_LINE);
                }
            }
        }

        sb.append(NEW_LINE).append("【完整响应 JSON】: ").append(NEW_LINE);
        sb.append(ModelOptionsUtils.toJsonString(response)).append(NEW_LINE);
        sb.append("=".repeat(54));

        return sb.toString();
    };

    private final Function<ChatClientRequest, String> requestToString;
    private final Function<ChatResponse, String> responseToString;
    private final int order;

    public PrettyLoggerAdvisor() {
        this(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, BaseAdvisor.HIGHEST_PRECEDENCE + 600);
    }

    public PrettyLoggerAdvisor(int order) {
        this(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, order);
    }

    public PrettyLoggerAdvisor(@Nullable Function<ChatClientRequest, String> requestToString,
                               @Nullable Function<ChatResponse, String> responseToString, int order) {
        this.requestToString = requestToString != null ? requestToString : DEFAULT_REQUEST_TO_STRING;
        this.responseToString = responseToString != null ? responseToString : DEFAULT_RESPONSE_TO_STRING;
        this.order = order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        logResponse(chatClientResponse);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        logRequest(chatClientRequest);
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }

    protected void logRequest(ChatClientRequest request) {
        logger.info(this.requestToString.apply(request));
    }

    protected void logResponse(ChatClientResponse chatClientResponse) {
        logger.info(this.responseToString.apply(chatClientResponse.chatResponse()));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Function<ChatClientRequest, String> requestToString;
        private Function<ChatResponse, String> responseToString;
        private int order = 0;

        public Builder requestToString(Function<ChatClientRequest, String> requestToString) {
            this.requestToString = requestToString;
            return this;
        }

        public Builder responseToString(Function<ChatResponse, String> responseToString) {
            this.responseToString = responseToString;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public PrettyLoggerAdvisor build() {
            return new PrettyLoggerAdvisor(this.requestToString, this.responseToString, this.order);
        }
    }
}
