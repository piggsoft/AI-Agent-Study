package com.piggsoft.webchat.service.custom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 手动构建 OpenAI function calling 规范的 tool 定义。
 * 等价于 Spring AI {@code @Tool} 注解的效果。
 */
@Component
@Slf4j
public class ToolDefinitionBuilder {

    public List<Map<String, Object>> buildToolDefinitions() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("orderNo", Map.of("type", "string", "description", "订单号"));
        properties.put("customer", Map.of("type", "string", "description", "客户名称"));
        properties.put("createdAt", Map.of("type", "string", "description", "创建时间"));

        Map<String, Object> function = new HashMap<>();
        function.put("name", "queryOrders");
        function.put("description", "根据参数查询订单信息。可传入orderNo, customer, createdAt");
        function.put("parameters", Map.of(
                "type", "object",
                "properties", properties
        ));

        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", function);

        log.info("[Custom] 已注册工具: queryOrders (手动 JSON Schema)");
        return List.of(tool);
    }
}
