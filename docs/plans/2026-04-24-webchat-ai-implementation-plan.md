# WebChat AI 系统实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 构建一个 WebChat 应用，连接 Java Spring Boot 后端，后端通过 Spring AI 调用 MiniMax 大模型，并在调用前从 MySQL
数据库获取业务数据作为上下文。

**Architecture:** 采用前后端分离架构，前端使用 Vite + React，通过 SSE 实现流式对话；后端使用 Spring Boot + MyBatis，通过
Spring AI 集成 MiniMax 模型。

**Tech Stack:** Vite, React 18, Zustand, Tailwind CSS | Spring Boot 3.2, MyBatis, Spring AI 1.0, MySQL

---

## 前置准备

### 任务 1: 初始化项目结构

**Files:**

- Create: `webchat-ai/pom.xml`
- Create: `webchat-ui/package.json`
- Create: `webchat-ui/vite.config.ts`
- Create: `webchat-ui/tailwind.config.js`
- Create: `webchat-ui/postcss.config.js`
- Create: `webchat-ui/index.html`
- Create: `docs/sql/init.sql`

**Step 1: 创建后端项目 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>webchat-ai</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>webchat-ai</name>
    <description>WebChat AI System with MiniMax</description>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>1.0.0-M6</spring-ai.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- MyBatis -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.3</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Spring AI -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-minimax-spring-boot-starter</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
</project>
```

**Step 2: 创建前端 package.json**

```json
{
  "name": "webchat-ui",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "zustand": "^4.5.2",
    "lucide-react": "^0.378.0",
    "date-fns": "^3.6.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.1",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.2.1",
    "autoprefixer": "^10.4.19",
    "postcss": "^8.4.38",
    "tailwindcss": "^3.4.3",
    "typescript": "^5.4.5",
    "vite": "^5.2.10",
    "@tailwindcss/typography": "^0.5.12"
  }
}
```

**Step 3: 创建 vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

**Step 4: 创建 tailwind.config.js**

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [
    require('@tailwindcss/typography'),
  ],
}
```

**Step 5: 创建 postcss.config.js**

```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

**Step 6: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>WebChat AI</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

**Step 7: 创建数据库初始化脚本**

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS webchat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE webchat;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no    VARCHAR(64)  NOT NULL COMMENT '订单号',
    customer    VARCHAR(128) COMMENT '客户名称',
    amount      DECIMAL(12,2) COMMENT '订单金额',
    status      VARCHAR(32)  COMMENT '订单状态',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order_no (order_no),
    INDEX idx_customer (customer),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 对话历史表
CREATE TABLE IF NOT EXISTS chat_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id      VARCHAR(64)  NOT NULL COMMENT '会话ID',
    role            VARCHAR(16)  NOT NULL COMMENT '角色: user/assistant',
    content         TEXT         NOT NULL COMMENT '对话内容',
    query_conditions JSON        COMMENT '查询条件JSON',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';

-- AI配置表
CREATE TABLE IF NOT EXISTS ai_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_key      VARCHAR(64)  NOT NULL UNIQUE COMMENT '配置键',
    config_value    TEXT         COMMENT '配置值',
    description     VARCHAR(256) COMMENT '配置描述',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI配置表';

-- 初始化配置数据
INSERT INTO ai_config (config_key, config_value, description) VALUES
('system_prompt', '你是一个数据分析助手，可以根据提供的业务数据回答用户问题。', '系统提示词'),
('model_name', 'MiniMax-Text-01', '模型名称');

-- 初始化测试订单数据
INSERT INTO orders (order_no, customer, amount, status, created_at) VALUES
('ORD202401001', '张三', 1500.00, '已完成', '2024-01-15 10:30:00'),
('ORD202401002', '李四', 2800.00, '处理中', '2024-01-16 14:20:00'),
('ORD202401003', '王五', 999.00, '已取消', '2024-01-17 09:15:00'),
('ORD202401004', '张三', 5600.00, '已完成', '2024-01-18 16:45:00'),
('ORD202401005', '赵六', 3200.00, '已完成', '2024-01-19 11:00:00');
```

**Step 8: 提交代码**

```bash
git add -A
git commit -m "chore: 初始化项目结构"
```

---

## 后端开发

### 任务 2: 后端基础配置

**Files:**

- Create: `webchat-ai/src/main/java/com/example/webchat/WebChatApplication.java`
- Create: `webchat-ai/src/main/resources/application.yml`
- Create: `webchat-ai/src/main/resources/application-dev.yml`

**Step 1: 创建启动类**

```java
package com.example.webchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebChatApplication.class, args);
    }
}
```

**Step 2: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: webchat-ai
  
  datasource:
    url: jdbc:mysql://localhost:3306/webchat?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  ai:
    minimax:
      api-key: ${MINIMAX_API_KEY:your-api-key}
      base-url: https://api.minimaxi.chat/v1

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.webchat.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

logging:
  level:
    com.example.webchat: DEBUG
```

**Step 3: 提交代码**

```bash
git add -A
git commit -m "feat: 添加后端基础配置"
```

---

### 任务 3: 后端实体类

**Files:**

- Create: `webchat-ai/src/main/java/com/example/webchat/entity/Order.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/entity/ChatHistory.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/entity/AiConfig.java`

**Step 1: 创建 Order 实体**

```java
package com.example.webchat.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private String customer;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Step 2: 创建 ChatHistory 实体**

```java
package com.example.webchat.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatHistory {
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String queryConditions;
    private LocalDateTime createdAt;
}
```

**Step 3: 创建 AiConfig 实体**

```java
package com.example.webchat.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;
}
```

**Step 4: 提交代码**

```bash
git add -A
git commit -m "feat: 添加实体类"
```

---

### 任务 4: 后端 Mapper 层

**Files:**

- Create: `webchat-ai/src/main/java/com/example/webchat/mapper/OrderMapper.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/mapper/ChatHistoryMapper.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/mapper/AiConfigMapper.java`
- Create: `webchat-ai/src/main/resources/mapper/OrderMapper.xml`
- Create: `webchat-ai/src/main/resources/mapper/ChatHistoryMapper.xml`
- Create: `webchat-ai/src/main/resources/mapper/AiConfigMapper.xml`

**Step 1: 创建 OrderMapper**

```java
package com.example.webchat.mapper;

import entity.com.piggsoft.webchat.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    
    List<Order> selectByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    List<Order> selectAll();
}
```

**Step 2: 创建 ChatHistoryMapper**

```java
package com.example.webchat.mapper;

import entity.com.piggsoft.webchat.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ChatHistoryMapper {
    
    List<ChatHistory> selectBySessionId(@Param("sessionId") String sessionId);
    
    int insert(ChatHistory chatHistory);
}
```

**Step 3: 创建 AiConfigMapper**

```java
package com.example.webchat.mapper;

import entity.com.piggsoft.webchat.AiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface AiConfigMapper {
    
    Optional<AiConfig> selectByConfigKey(@Param("configKey") String configKey);
}
```

**Step 4: 创建 OrderMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="mapper.com.piggsoft.webchat.OrderMapper">

    <select id="selectByDateRange" resultType="entity.com.piggsoft.webchat.Order">
        SELECT id, order_no, customer, amount, status, created_at, updated_at
        FROM orders
        WHERE created_at BETWEEN #{startDate} AND #{endDate}
        ORDER BY created_at DESC
    </select>

    <select id="selectAll" resultType="entity.com.piggsoft.webchat.Order">
        SELECT id, order_no, customer, amount, status, created_at, updated_at
        FROM orders
        ORDER BY created_at DESC
    </select>

</mapper>
```

**Step 5: 创建 ChatHistoryMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="mapper.com.piggsoft.webchat.ChatHistoryMapper">

    <select id="selectBySessionId" resultType="entity.com.piggsoft.webchat.ChatHistory">
        SELECT id, session_id, role, content, query_conditions, created_at
        FROM chat_history
        WHERE session_id = #{sessionId}
        ORDER BY created_at ASC
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO chat_history (session_id, role, content, query_conditions, created_at)
        VALUES (#{sessionId}, #{role}, #{content}, #{queryConditions}, NOW())
    </insert>

</mapper>
```

**Step 6: 创建 AiConfigMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="mapper.com.piggsoft.webchat.AiConfigMapper">

    <select id="selectByConfigKey" resultType="entity.com.piggsoft.webchat.AiConfig">
        SELECT id, config_key, config_value, description, updated_at
        FROM ai_config
        WHERE config_key = #{configKey}
    </select>

</mapper>
```

**Step 7: 提交代码**

```bash
git add -A
git commit -m "feat: 添加 Mapper 层"
```

---

### 任务 5: 后端 DTO 类

**Files:**

- Create: `webchat-ai/src/main/java/com/example/webchat/dto/ChatRequest.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/dto/ChatResponse.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/dto/ApiResponse.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/dto/ChatHistoryDTO.java`

**Step 1: 创建 ChatRequest**

```java
package com.example.webchat.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ChatRequest {
    private String sessionId;
    private String message;
    private Map<String, Object> queryConditions;
}
```

**Step 2: 创建 ChatResponse**

```java
package com.example.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String content;
    private boolean done;
}
```

**Step 3: 创建 ApiResponse**

```java
package com.example.webchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }
}
```

**Step 4: 创建 ChatHistoryDTO**

```java
package com.example.webchat.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatHistoryDTO {
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private Object queryConditions;
    private LocalDateTime createdAt;
}
```

**Step 5: 提交代码**

```bash
git add -A
git commit -m "feat: 添加 DTO 类"
```

---

### 任务 6: 后端 Service 层

**Files:**

- Create: `webchat-ai/src/main/java/com/example/webchat/service/BusinessDataService.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/service/BusinessDataServiceImpl.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/service/PromptBuilder.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/service/ChatService.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/service/ChatServiceImpl.java`

**Step 1: 创建 BusinessDataService**

```java
package com.example.webchat.service;

import entity.com.piggsoft.webchat.Order;
import java.util.List;
import java.util.Map;

public interface BusinessDataService {
    
    List<Order> queryOrders(Map<String, Object> conditions);
    
    List<Order> queryAllOrders();
}
```

**Step 2: 创建 BusinessDataServiceImpl**

```java
package com.example.webchat.service;

import entity.com.piggsoft.webchat.Order;
import mapper.com.piggsoft.webchat.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusinessDataServiceImpl implements BusinessDataService {

    private final OrderMapper orderMapper;

    @Override
    public List<Order> queryOrders(Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return queryAllOrders();
        }
        
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (conditions.containsKey("startDate")) {
            String start = (String) conditions.get("startDate");
            startDate = LocalDate.parse(start).atStartOfDay();
        }
        
        if (conditions.containsKey("endDate")) {
            String end = (String) conditions.get("endDate");
            endDate = LocalDate.parse(end).atTime(LocalTime.MAX);
        }
        
        if (startDate != null && endDate != null) {
            return orderMapper.selectByDateRange(startDate, endDate);
        }
        
        return queryAllOrders();
    }

    @Override
    public List<Order> queryAllOrders() {
        return orderMapper.selectAll();
    }
}
```

**Step 3: 创建 PromptBuilder**

```java
package com.example.webchat.service;

import entity.com.piggsoft.webchat.AiConfig;
import entity.com.piggsoft.webchat.Order;
import mapper.com.piggsoft.webchat.AiConfigMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private final AiConfigMapper aiConfigMapper;
    private final ObjectMapper objectMapper;

    public String buildPrompt(String userMessage, Map<String, Object> queryConditions, List<Order> businessData) {
        String systemPrompt = getSystemPrompt();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("【系统角色】\n").append(systemPrompt).append("\n\n");
        
        if (queryConditions != null && !queryConditions.isEmpty()) {
            prompt.append("【用户查询条件】\n");
            try {
                prompt.append(objectMapper.writeValueAsString(queryConditions));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize query conditions", e);
            }
            prompt.append("\n\n");
        }
        
        if (businessData != null && !businessData.isEmpty()) {
            prompt.append("【相关业务数据】\n");
            try {
                prompt.append(objectMapper.writeValueAsString(businessData));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize business data", e);
            }
            prompt.append("\n\n");
        }
        
        prompt.append("【用户问题】\n").append(userMessage).append("\n\n");
        prompt.append("【回答要求】\n");
        prompt.append("1. 基于以上业务数据回答用户问题\n");
        prompt.append("2. 如果数据不足或无法回答，请明确告知\n");
        prompt.append("3. 回答要简洁、准确\n");
        
        return prompt.toString();
    }

    private String getSystemPrompt() {
        Optional<AiConfig> config = aiConfigMapper.selectByConfigKey("system_prompt");
        return config.map(AiConfig::getConfigValue)
                .orElse("你是一个数据分析助手，可以根据提供的业务数据回答用户问题。");
    }
}
```

**Step 4: 创建 ChatService**

```java
package com.example.webchat.service;

import dto.com.piggsoft.webchat.ChatRequest;
import org.springframework.ai.chat.model.ChatResponse as SpringChatResponse;
import reactor.core.publisher.Flux;

public interface ChatService {
    
    Flux<String> chatStream(ChatRequest request);
}
```

**Step 5: 创建 ChatServiceImpl**

```java
package com.example.webchat.service;

import dto.com.piggsoft.webchat.ChatRequest;
import entity.com.piggsoft.webchat.ChatHistory;
import entity.com.piggsoft.webchat.Order;
import mapper.com.piggsoft.webchat.ChatHistoryMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.model.ChatResponseMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final PromptBuilder promptBuilder;
    private final BusinessDataService businessDataService;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        log.info("Processing chat request: sessionId={}, message={}", 
                request.getSessionId(), request.getMessage());
        
        List<Order> businessData = businessDataService.queryOrders(request.getQueryConditions());
        
        String prompt = promptBuilder.buildPrompt(
                request.getMessage(), 
                request.getQueryConditions(), 
                businessData
        );
        
        chatHistoryMapper.insert(buildChatHistory(request.getSessionId(), "user", request.getMessage(), request.getQueryConditions()));
        
        Flux<String> responseFlux = chatClient.prompt()
                .user(prompt)
                .stream()
                .chatResponseStream()
                .map(response -> {
                    ChatResponseMessage content = response.getResult().getOutput();
                    return content != null ? content.getText() : "";
                })
                .filter(text -> !text.isEmpty());
        
        return responseFlux;
    }

    private ChatHistory buildChatHistory(String sessionId, String role, String content, Object queryConditions) {
        ChatHistory history = new ChatHistory();
        history.setSessionId(sessionId);
        history.setRole(role);
        history.setContent(content);
        if (queryConditions != null) {
            try {
                history.setQueryConditions(objectMapper.writeValueAsString(queryConditions));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize query conditions", e);
            }
        }
        return history;
    }
}
```

**Step 6: 提交代码**

```bash
git add -A
git commit -m "feat: 添加 Service 层"
```

---

### 任务 7: 后端 Controller 层

**Files:**

- Create: `webchat-ai/src/main/java/com/example/webchat/controller/ChatController.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/config/SpringAIConfig.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/config/CorsConfig.java`
- Create: `webchat-ai/src/main/java/com/example/webchat/config/JacksonConfig.java`

**Step 1: 创建 SpringAIConfig**

```java
package com.example.webchat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
```

**Step 2: 创建 CorsConfig**

```java
package com.example.webchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
```

**Step 3: 创建 JacksonConfig**

```java
package com.example.webchat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

**Step 4: 创建 ChatController**

```java
package com.example.webchat.controller;

import dto.com.piggsoft.webchat.ApiResponse;
import dto.com.piggsoft.webchat.ChatHistoryDTO;
import dto.com.piggsoft.webchat.ChatRequest;
import entity.com.piggsoft.webchat.ChatHistory;
import mapper.com.piggsoft.webchat.ChatHistoryMapper;
import service.com.piggsoft.webchat.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        StringBuilder sseBuilder = new StringBuilder();
        
        return chatService.chatStream(request)
                .map(content -> {
                    sseBuilder.append(content);
                    return "data: " + content + "\n\n";
                })
                .doOnComplete(() -> {
                    String fullResponse = sseBuilder.toString();
                    log.info("Chat completed, saving response to history");
                    ChatHistory history = new ChatHistory();
                    history.setSessionId(request.getSessionId());
                    history.setRole("assistant");
                    history.setContent(fullResponse);
                    chatHistoryMapper.insert(history);
                })
                .startWith("data: \n\n");
    }

    @GetMapping("/history/{sessionId}")
    public ApiResponse<List<ChatHistoryDTO>> getHistory(@PathVariable String sessionId) {
        List<ChatHistory> histories = chatHistoryMapper.selectBySessionId(sessionId);
        
        List<ChatHistoryDTO> dtos = histories.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ApiResponse.success(dtos);
    }

    private ChatHistoryDTO toDTO(ChatHistory history) {
        ChatHistoryDTO dto = new ChatHistoryDTO();
        dto.setId(history.getId());
        dto.setSessionId(history.getSessionId());
        dto.setRole(history.getRole());
        dto.setContent(history.getContent());
        dto.setCreatedAt(history.getCreatedAt());
        
        if (history.getQueryConditions() != null) {
            try {
                dto.setQueryConditions(objectMapper.readValue(history.getQueryConditions(), Object.class));
            } catch (JsonProcessingException e) {
                dto.setQueryConditions(history.getQueryConditions());
            }
        }
        
        return dto;
    }
}
```

**Step 5: 提交代码**

```bash
git add -A
git commit -m "feat: 添加 Controller 层和配置"
```

---

## 前端开发

### 任务 8: 前端基础配置

**Files:**

- Create: `webchat-ui/src/main.tsx`
- Create: `webchat-ui/src/index.css`
- Create: `webchat-ui/src/App.tsx`
- Create: `webchat-ui/src/vite-env.d.ts`

**Step 1: 创建 main.tsx**

```tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

**Step 2: 创建 index.css**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  margin: 0;
  padding: 0;
  background-color: #f5f5f5;
}

.scrollbar-thin::-webkit-scrollbar {
  width: 6px;
}

.scrollbar-thin::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.scrollbar-thin::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}
```

**Step 3: 创建 App.tsx**

```tsx
import ChatContainer from './components/ChatContainer'

function App() {
  return (
    <div className="min-h-screen bg-gray-100">
      <ChatContainer />
    </div>
  )
}

export default App
```

**Step 4: 创建 vite-env.d.ts**

```typescript
/// <reference types="vite/client" />
```

**Step 5: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

**Step 6: 创建 tsconfig.node.json**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

**Step 7: 提交代码**

```bash
git add -A
git commit -m "feat: 前端基础配置"
```

---

### 任务 9: 前端类型定义和状态管理

**Files:**

- Create: `webchat-ui/src/types/chat.ts`
- Create: `webchat-ui/src/store/chatStore.ts`
- Create: `webchat-ui/src/api/chatApi.ts`

**Step 1: 创建 chat.ts**

```typescript
export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  queryConditions?: QueryConditions
  timestamp: Date
}

export interface QueryConditions {
  startDate?: string
  endDate?: string
  [key: string]: string | undefined
}

export interface ChatRequest {
  sessionId: string
  message: string
  queryConditions?: QueryConditions
}

export interface ChatHistory {
  id: number
  sessionId: string
  role: string
  content: string
  queryConditions?: QueryConditions
  createdAt: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
```

**Step 2: 创建 chatStore.ts**

```typescript
import { create } from 'zustand'
import { Message, QueryConditions } from '../types/chat'

interface ChatState {
  sessionId: string
  messages: Message[]
  isLoading: boolean
  queryConditions: QueryConditions
  setSessionId: (sessionId: string) => void
  addMessage: (message: Message) => void
  updateLastMessage: (content: string) => void
  setLoading: (isLoading: boolean) => void
  setQueryConditions: (conditions: QueryConditions) => void
  clearMessages: () => void
}

export const useChatStore = create<ChatState>((set) => ({
  sessionId: crypto.randomUUID(),
  messages: [],
  isLoading: false,
  queryConditions: {},
  setSessionId: (sessionId) => set({ sessionId }),
  addMessage: (message) => set((state) => ({ 
    messages: [...state.messages, message] 
  })),
  updateLastMessage: (content) => set((state) => {
    const messages = [...state.messages]
    if (messages.length > 0) {
      messages[messages.length - 1].content += content
    }
    return { messages }
  }),
  setLoading: (isLoading) => set({ isLoading }),
  setQueryConditions: (conditions) => set({ queryConditions: conditions }),
  clearMessages: () => set({ messages: [], sessionId: crypto.randomUUID() }),
}))
```

**Step 3: 创建 chatApi.ts**

```typescript
import {ChatRequest, ChatHistory, ApiResponse} from '../types/chat'

const API_BASE = '/api/chat'

export async function sendChatStream(
    request: ChatRequest,
    onChunk: (content: string) => void,
    onDone: () => void,
    onError: (error: Error) => void
): Promise<void> {
    try {
        const response = await fetch(`${API_BASE}/stream`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        })

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
            throw new Error('No response body')
        }

        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
            const {done, value} = await reader.read()
            if (done) break

            buffer += decoder.decode(value, {stream: true})

            while (buffer.includes('\n')) {
                const newlineIndex = buffer.indexOf('\n')
                const line = buffer.slice(0, newlineIndex).trim()
                buffer = buffer.slice(newlineIndex + 1)

                if (line.startsWith('data: ')) {
                    const data = line.slice(6).trim()
                    if (data === '') {
                        continue
                    }
                    if (data === '[DONE]') {
                        break
                    }
                    onChunk(data)
                }
            }
        }

        onDone()
    } catch (error) {
        onError(error as Error)
    }
}

export async function getChatHistory(sessionId: string): Promise<ChatHistory[]> {
    const response = await fetch(`${API_BASE}/history/${sessionId}`)
    const result: ApiResponse<ChatHistory[]> = await response.json()

    if (result.code !== 200) {
        throw new Error(result.message)
    }

    return result.data
}
```

**Step 4: 提交代码**

```bash
git add -A
git commit -m "feat: 前端类型定义和状态管理"
```

---

### 任务 10: 前端组件开发

**Files:**

- Create: `webchat-ui/src/components/ChatContainer.tsx`
- Create: `webchat-ui/src/components/ChatMessage.tsx`
- Create: `webchat-ui/src/components/ChatInput.tsx`
- Create: `webchat-ui/src/components/QueryConditionPanel.tsx`

**Step 1: 创建 ChatContainer.tsx**

```tsx
import { useEffect } from 'react'
import { useChatStore } from '../store/chatStore'
import { sendChatStream } from '../api/chatApi'
import { Message } from '../types/chat'
import ChatMessage from './ChatMessage'
import ChatInput from './ChatInput'
import QueryConditionPanel from './QueryConditionPanel'

export default function ChatContainer() {
  const { 
    sessionId, 
    messages, 
    isLoading, 
    queryConditions,
    addMessage, 
    updateLastMessage, 
    setLoading,
    clearMessages 
  } = useChatStore()

  useEffect(() => {
    console.log('Session ID:', sessionId)
  }, [sessionId])

  const handleSend = async (content: string) => {
    if (!content.trim() || isLoading) return

    const userMessage: Message = {
      id: crypto.randomUUID(),
      role: 'user',
      content,
      queryConditions: Object.keys(queryConditions).length > 0 ? queryConditions : undefined,
      timestamp: new Date(),
    }
    addMessage(userMessage)
    setLoading(true)

    const assistantMessage: Message = {
      id: crypto.randomUUID(),
      role: 'assistant',
      content: '',
      timestamp: new Date(),
    }
    addMessage(assistantMessage)

    await sendChatStream(
      {
        sessionId,
        message: content,
        queryConditions: Object.keys(queryConditions).length > 0 ? queryConditions : undefined,
      },
      (chunk) => updateLastMessage(chunk),
      () => setLoading(false),
      (error) => {
        console.error('Chat error:', error)
        updateLastMessage(`\n\n[Error: ${error.message}]`)
        setLoading(false)
      }
    )
  }

  return (
    <div className="flex flex-col h-screen max-w-4xl mx-auto bg-white shadow-xl">
      <header className="px-6 py-4 bg-blue-600 text-white">
        <h1 className="text-xl font-bold">WebChat AI</h1>
      </header>

      <QueryConditionPanel />

      <div className="flex-1 overflow-y-auto p-6 space-y-4 scrollbar-thin">
        {messages.length === 0 && (
          <div className="text-center text-gray-500 mt-20">
            <p className="text-lg">开始对话吧！</p>
            <p className="text-sm mt-2">输入你的问题，AI 将根据业务数据为你解答</p>
          </div>
        )}
        {messages.map((message) => (
          <ChatMessage key={message.id} message={message} />
        ))}
      </div>

      <div className="border-t p-4">
        <ChatInput onSend={handleSend} disabled={isLoading} />
      </div>
    </div>
  )
}
```

**Step 2: 创建 ChatMessage.tsx**

```tsx
import { Message } from '../types/chat'
import { format } from 'date-fns'
import { zhCN } from 'date-fns/locale'

interface ChatMessageProps {
  message: Message
}

export default function ChatMessage({ message }: ChatMessageProps) {
  const isUser = message.role === 'user'

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[70%] rounded-lg p-4 ${
          isUser
            ? 'bg-blue-600 text-white'
            : 'bg-gray-100 text-gray-800'
        }`}
      >
        {message.queryConditions && (
          <div className={`text-xs mb-2 p-2 rounded ${
            isUser ? 'bg-blue-500' : 'bg-gray-200'
          }`}>
            <span className="font-semibold">查询条件：</span>
            {JSON.stringify(message.queryConditions)}
          </div>
        )}
        <div className="whitespace-pre-wrap">{message.content}</div>
        <div className={`text-xs mt-2 ${isUser ? 'text-blue-200' : 'text-gray-500'}`}>
          {format(new Date(message.timestamp), 'HH:mm:ss', { locale: zhCN })}
        </div>
      </div>
    </div>
  )
}
```

**Step 3: 创建 ChatInput.tsx**

```tsx
import { useState, FormEvent } from 'react'
import { Send } from 'lucide-react'

interface ChatInputProps {
  onSend: (content: string) => void
  disabled?: boolean
}

export default function ChatInput({ onSend, disabled }: ChatInputProps) {
  const [input, setInput] = useState('')

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    if (input.trim()) {
      onSend(input.trim())
      setInput('')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <input
        type="text"
        value={input}
        onChange={(e) => setInput(e.target.value)}
        disabled={disabled}
        placeholder={disabled ? 'AI 正在思考...' : '输入你的问题...'}
        className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
      />
      <button
        type="submit"
        disabled={disabled || !input.trim()}
        className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
      >
        <Send size={18} />
        发送
      </button>
    </form>
  )
}
```

**Step 4: 创建 QueryConditionPanel.tsx**

```tsx
import { useState } from 'react'
import { useChatStore } from '../store/chatStore'
import { Filter, X } from 'lucide-react'

export default function QueryConditionPanel() {
  const { queryConditions, setQueryConditions } = useChatStore()
  const [isExpanded, setIsExpanded] = useState(false)
  const [localConditions, setLocalConditions] = useState({
    startDate: queryConditions.startDate || '',
    endDate: queryConditions.endDate || '',
  })

  const handleApply = () => {
    const conditions: Record<string, string> = {}
    if (localConditions.startDate) conditions.startDate = localConditions.startDate
    if (localConditions.endDate) conditions.endDate = localConditions.endDate
    setQueryConditions(conditions)
    setIsExpanded(false)
  }

  const handleClear = () => {
    setQueryConditions({})
    setLocalConditions({ startDate: '', endDate: '' })
  }

  const hasConditions = Object.keys(queryConditions).length > 0

  return (
    <div className="border-b bg-gray-50">
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full px-6 py-3 flex items-center justify-between text-gray-700 hover:bg-gray-100 transition-colors"
      >
        <div className="flex items-center gap-2">
          <Filter size={18} />
          <span className="font-medium">查询条件</span>
          {hasConditions && (
            <span className="px-2 py-0.5 bg-blue-100 text-blue-700 text-xs rounded-full">
              已设置
            </span>
          )}
        </div>
        <span className="text-sm">{isExpanded ? '收起' : '展开'}</span>
      </button>

      {isExpanded && (
        <div className="px-6 pb-4 space-y-4">
          <div className="flex gap-4">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-600 mb-1">
                开始日期
              </label>
              <input
                type="date"
                value={localConditions.startDate}
                onChange={(e) => setLocalConditions({ ...localConditions, startDate: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-600 mb-1">
                结束日期
              </label>
              <input
                type="date"
                value={localConditions.endDate}
                onChange={(e) => setLocalConditions({ ...localConditions, endDate: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div className="flex gap-2">
            <button
              onClick={handleApply}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              应用条件
            </button>
            <button
              onClick={handleClear}
              className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors flex items-center gap-1"
            >
              <X size={16} />
              清除
            </button>
          </div>

          {hasConditions && (
            <div className="text-sm text-gray-500">
              当前条件：{JSON.stringify(queryConditions)}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
```

**Step 5: 提交代码**

```bash
git add -A
git commit -m "feat: 前端组件开发"
```

---

## 任务 11: README 文档

**Files:**

- Create: `README.md`
- Create: `webchat-ai/README.md`
- Create: `webchat-ui/README.md`

**Step 1: 创建根目录 README.md**

```markdown
# WebChat AI System

一个基于 Spring Boot + React 的智能对话系统，连接 MiniMax 大模型，支持业务数据上下文注入。

## 项目结构

```

webchat-ai/ # 后端 Spring Boot 项目
webchat-ui/ # 前端 React 项目
docs/ # 文档目录

```

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < docs/sql/init.sql
```

### 2. 启动后端

```bash
cd webchat-ai
./mvnw spring-boot:run
```

### 3. 启动前端

```bash
cd webchat-ui
npm install
npm run dev
```

### 4. 配置环境变量

后端需要设置以下环境变量：

```bash
export MINIMAX_API_KEY=your-api-key
export DB_PASSWORD=your-db-password
```

## 技术栈

- **前端**: React 18, Vite, Tailwind CSS, Zustand
- **后端**: Spring Boot 3.2, Spring AI 1.0, MyBatis
- **数据库**: MySQL 8.x
- **AI**: MiniMax (通过 NVIDIA Build)

```

**Step 2: 提交代码**

```bash
git add -A
git commit -m "docs: 添加 README 文档"
```

---

## 总结

完成以上 11 个任务后，你将拥有：

1. ✅ 完整的前后端项目结构
2. ✅ 数据库表设计和初始化脚本
3. ✅ 后端完整的分层架构（Controller, Service, Mapper）
4. ✅ 前端 React 组件和状态管理
5. ✅ SSE 流式对话功能
6. ✅ 业务数据查询和上下文注入
7. ✅ 文档和 README

## 运行顺序

1. 初始化数据库
2. 启动后端（端口 8080）
3. 启动前端（端口 5173）
4. 打开浏览器访问 http://localhost:5173
