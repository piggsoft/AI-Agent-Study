package com.piggsoft.webchat.service.custom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggsoft.webchat.entity.Order;
import com.piggsoft.webchat.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 根据 LLM 返回的函数名和参数，本地执行对应的方法。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolExecutor {

    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    public String executeTool(String toolName, String arguments) {
        try {
            if ("queryOrders".equals(toolName)) {
                Order queryOrder = objectMapper.readValue(arguments, Order.class);
                log.info("[Custom] 执行 queryOrders, 查询条件: customer={}, orderNo={}, createdAt={}",
                        queryOrder.getCustomer(), queryOrder.getOrderNo(), queryOrder.getCreatedAt());
                List<Order> results = orderMapper.query(queryOrder);
                log.info("[Custom] queryOrders 查询结果: 共 {} 条记录", results.size());
                return objectMapper.writeValueAsString(results);
            }
            return objectMapper.writeValueAsString(Map.of("error", "Unknown tool: " + toolName));
        } catch (JsonProcessingException e) {
            log.error("[Custom] 工具执行失败: toolName={}, arguments={}", toolName, arguments, e);
            return "{\"error\": \"工具执行异常: " + e.getMessage() + "\"}";
        }
    }
}
