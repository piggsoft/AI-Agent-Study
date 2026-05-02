# WebChat AI 对话系统设计文档

## 1. 项目概述

### 1.1 项目背景

构建一个 WebChat 前端应用，连接 Java
后端服务，后端服务通过调用大模型（MiniMax）提供智能对话能力。在调用大模型之前，后端从数据库获取相关业务数据，将其作为上下文一起发送给大模型，增强回答的准确性和相关性。

### 1.2 项目目标

- 提供流畅的流式对话体验
- 支持用户指定查询条件，查询业务数据
- 将业务数据作为上下文注入大模型 Prompt
- 实现对话历史记录功能
- 简单易部署（单机部署）

---

## 2. 技术选型

### 2.1 技术栈总览

| 层级　　　　   | 技术　　　　　　　　　　　　              | 版本　   | 说明　　　　　　　　　　    |
|----------|-----------------------------|-------|-----------------|
| 前端构建　　   | Vite　　　　　　　　　　　　            | 5.x　  | 快速开发服务器和构建工具    |
| 前端框架　　   | React　　　　　　　　　　　            | 18.x　 | UI 组件化框架　　　　　　  |
| 前端 UI　　　 | Tailwind CSS　　　　　　　　        | 3.x　  | 原子化 CSS 框架　　　　　 |
| 前端状态管理   | Zustand　　　　　　　　　　           | 4.x　  | 轻量级状态管理　　　　　    |
| 后端框架　　   | Spring Boot　　　　　　　　         | 3.2.x | Java 主流框架　　　　　　 |
| ORM　　　　　 | MyBatis Spring Boot Starter | 3.0.x | 数据访问层　　　　　　　    |
| AI 集成　　　 | Spring AI　　　　　　　　　          | 1.0.x | 大模型统一抽象层　　　　    |
| 数据库　　　   | MySQL　　　　　　　　　　　            | 8.x　  | 关系型数据库　　　　　　    |
| 流式通信　　   | SSE　　　　　　　　　　　　             | -　　   | 服务端推送流式响应　　　    |
| 项目构建　　   | Maven　　　　　　　　　　　            | 3.9.x | 后端构建　　　　　　　　    |
| 前端包管理　   | npm/pnpm　　　　　　　　　　          | -　　   | 前端依赖管理　　　　　　    |

### 2.2 依赖说明

#### 后端依赖 (pom.xml)

```xml
- spring-boot-starter-web
- spring-boot-starter-webflux (用于 SSE 流式响应)
- spring-ai-starter-model-minimax (Spring AI MiniMax 支持)
- mybatis-spring-boot-starter
- mysql-connector-j
- lombok
- spring-boot-starter-test
```

#### 前端依赖 (package.json)

```json
- react
- react-dom
- zustand (状态管理)
- tailwindcss
- @tailwindcss/typography
- lucide-react (图标库)
- date-fns (日期处理)
```

---

## 3. 系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              用户                                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │      HTTP POST /api/chat      │
                    │        SSE /api/chat/stream   │
                    ▼                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           React 前端                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │
│  │  ChatInput      │  │  ChatMessage     │  │  QueryConditionPanel    │ │
│  │  用户输入组件    │  │  消息展示组件    │  │  查询条件选择器          │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot 后端                                │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                         ChatController                              │ │
│  │   POST /api/chat/stream        GET /api/chat/history/{sessionId}   │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                    │
│  ┌─────────────────────────────────▼─────────────────────────────────┐  │
│  │                           ChatService                              │  │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐   │  │
│  │  │ PromptBuilder  │  │BusinessData    │  │ AiChatService      │   │  │
│  │  │ 提示词构建器    │──▶│ QueryService   │──│ AI对话服务(流式)    │   │  │
│  │  └────────────────┘  │ 业务数据查询     │  └────────────────────┘   │  │
│  │                      └────────────────┘                          │  │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                    │
│  ┌─────────────────────────────────▼─────────────────────────────────┐  │
│  │                        Repository 层                               │  │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐    │  │
│  │  │ OrderMapper    │  │ChatHistory     │  │ BusinessData       │    │  │
│  │  │ 订单数据访问    │  │Mapper          │  │ Mapper             │    │  │
│  │  └────────────────┘  └────────────────┘  └────────────────────┘    │  │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                    │
│                                    ▼                                    │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                          MySQL 数据库                               │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐  │ │
│  │  │ orders        │  │ chat_history   │  │ ai_config           │  │ │
│  │  │ 订单表         │  │ 对话历史表      │  │ AI配置表            │  │ │
│  │  └────────────────┘  └────────────────┘  └────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                      Spring AI 抽象层                               │ │
│  │  ┌──────────────────────────────────────────────────────────────┐ │ │
│  │  │              MiniMax Chat Model (NVIDIA Build)               │ │ │
│  │  └──────────────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTPS
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        MiniMax API (NVIDIA Build)                       │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                          MiniMax 大模型                            │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 数据流转图

```
1. 用户在 WebChat 输入消息并选择查询条件
         │
         ▼
2. 前端发送 POST /api/chat/stream 请求
         │
         ▼
3. 后端 Controller 接收请求
         │
         ▼
4. ChatService 调用 BusinessDataService 查询业务数据
         │
         ▼
5. 构建增强 Prompt:
   ┌──────────────────────────────────────────────────────────────┐
   │ System: 角色设定                                              │
   │ User:   用户问题 + 业务数据 JSON 上下文                        │
   └──────────────────────────────────────────────────────────────┘
         │
         ▼
6. 调用 Spring AI MiniMax ChatModel (流式)
         │
         ▼
7. SSE 流式响应返回前端
         │
         ▼
8. 前端实时渲染 AI 回复
         │
         ▼
9. 保存对话历史到数据库
```

---

## 4. 数据库设计

### 4.1 ER 图

```
┌─────────────────┐       ┌─────────────────┐
│    orders       │       │  chat_history   │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)         │
│ order_no        │       │ session_id      │
│ customer        │       │ role            │
│ amount          │       │ content          │
│ status          │       │ query_conditions│
│ created_at      │       │ created_at      │
│ updated_at      │       └─────────────────┘
└─────────────────┘               ▲
                                  │
                                  │ 通过 session_id 关联

┌─────────────────┐
│   ai_config     │
├─────────────────┤
│ id (PK)         │
│ config_key      │
│ config_value    │
│ description     │
│ updated_at      │
└─────────────────┘
```

### 4.2 表结构 DDL

#### 4.2.1 业务数据表 - orders（订单表）

```sql
CREATE TABLE orders (
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
```

#### 4.2.2 对话历史表 - chat_history

```sql
CREATE TABLE chat_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id      VARCHAR(64)  NOT NULL COMMENT '会话ID',
    role            VARCHAR(16)  NOT NULL COMMENT '角色: user/assistant',
    content         TEXT         NOT NULL COMMENT '对话内容',
    query_conditions JSON        COMMENT '查询条件JSON',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';
```

#### 4.2.3 系统配置表 - ai_config

```sql
CREATE TABLE ai_config (
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
```

### 4.3 业务数据表示例

```sql
-- 订单测试数据
INSERT INTO orders (order_no, customer, amount, status, created_at) VALUES
('ORD202401001', '张三', 1500.00, '已完成', '2024-01-15 10:30:00'),
('ORD202401002', '李四', 2800.00, '处理中', '2024-01-16 14:20:00'),
('ORD202401003', '王五', 999.00, '已取消', '2024-01-17 09:15:00'),
('ORD202401004', '张三', 5600.00, '已完成', '2024-01-18 16:45:00'),
('ORD202401005', '赵六', 3200.00, '已完成', '2024-01-19 11:00:00');
```

---

## 5. API 设计

### 5.1 发送消息（流式）

**接口**: `POST /api/chat/stream`

**请求头**:

```
Content-Type: application/json
```

**请求体**:

```json
{
  "sessionId": "uuid-xxx-xxx",
  "message": "帮我分析下本月订单",
  "queryConditions": {
    "startDate": "2024-01-01",
    "endDate": "2024-01-31"
  }
}
```

**响应**: `SSE (text/event-stream)`

```
HTTP/1.1 200 OK
Content-Type: text/event-stream

data: {"content": "好的", "done": false}
data: {"content": "，让我", "done": false}
data: {"content": "分析一下...", "done": false}
data: {"content": "根据您提供的订单数据...", "done": true}

```

**响应字段说明**:

| 字段      | 类型      | 说明                 |
|---------|---------|--------------------|
| content | string  | 流式返回的文本片段          |
| done    | boolean | 是否完成，true 表示流式输出结束 |

**错误响应**:

```json
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{
  "code": 500,
  "message": "AI 服务调用失败",
  "error": "具体错误信息"
}
```

---

### 5.2 获取历史消息

**接口**: `GET /api/chat/history/{sessionId}`

**路径参数**:

| 参数        | 类型     | 必填 | 说明   |
|-----------|--------|----|------|
| sessionId | string | 是  | 会话ID |

**响应**:

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "code": 200,
  "data": [
    {
      "id": 1,
      "sessionId": "uuid-xxx-xxx",
      "role": "user",
      "content": "帮我分析下本月订单",
      "queryConditions": {"startDate": "2024-01-01", "endDate": "2024-01-31"},
      "createdAt": "2024-01-20 10:30:00"
    },
    {
      "id": 2,
      "sessionId": "uuid-xxx-xxx",
      "role": "assistant",
      "content": "根据您提供的订单数据，本月共有5笔订单...",
      "queryConditions": null,
      "createdAt": "2024-01-20 10:30:05"
    }
  ]
}
```

---

## 6. 模块设计

### 6.1 后端模块结构

```
src/main/java/com/example/webchat/
├── WebChatApplication.java              # 应用入口
├── config/
│   └── SpringAIConfig.java              # Spring AI 配置
├── controller/
│   └── ChatController.java              # 对话控制器
├── service/
│   ├── ChatService.java                 # 对话服务接口
│   ├── ChatServiceImpl.java             # 对话服务实现
│   ├── BusinessDataService.java         # 业务数据查询服务
│   └── PromptBuilder.java               # Prompt 构建器
├── mapper/
│   ├── OrderMapper.java                 # 订单 Mapper
│   ├── ChatHistoryMapper.java           # 对话历史 Mapper
│   └── AiConfigMapper.java              # AI 配置 Mapper
├── entity/
│   ├── Order.java                       # 订单实体
│   ├── ChatHistory.java                 # 对话历史实体
│   └── AiConfig.java                    # AI 配置实体
├── dto/
│   ├── ChatRequest.java                 # 聊天请求 DTO
│   ├── ChatResponse.java                # 聊天响应 DTO
│   └── ChatHistoryDTO.java              # 对话历史 DTO
└── util/
    └── JsonUtil.java                    # JSON 工具类
```

### 6.2 前端模块结构

```
src/
├── components/
│   ├── ChatInput.tsx                    # 用户输入组件
│   ├── ChatMessage.tsx                  # 消息展示组件
│   ├── QueryConditionPanel.tsx          # 查询条件面板
│   └── ChatContainer.tsx               # 聊天容器
├── hooks/
│   └── useChat.ts                       # 聊天逻辑 Hook
├── store/
│   └── chatStore.ts                     # Zustand 状态管理
├── api/
│   └── chatApi.ts                       # API 调用
├── types/
│   └── chat.ts                          # TypeScript 类型定义
├── App.tsx
├── main.tsx
└── index.css
```

### 6.3 核心模块职责

| 模块         | 类/组件                | 职责                  |
|------------|---------------------|---------------------|
| Controller | ChatController      | 接收 HTTP 请求，返回 SSE 流 |
| Service    | ChatService         | 对话业务流程编排，异常处理       |
| Service    | BusinessDataService | 根据查询条件查询业务数据        |
| Service    | PromptBuilder       | 构建发送给 AI 的 Prompt   |
| Mapper     | OrderMapper         | 订单数据访问              |
| Mapper     | ChatHistoryMapper   | 对话历史数据访问            |
| 前端 Hook    | useChat             | 管理聊天状态、发送请求、处理 SSE  |

---

## 7. Prompt 设计

### 7.1 Prompt 模板

```
【系统角色】
{system_prompt}

【用户查询条件】
{query_conditions_json}

【相关业务数据】
{business_data_json}

【用户问题】
{user_message}

【回答要求】
1. 基于以上业务数据回答用户问题
2. 如果数据不足或无法回答，请明确告知
3. 回答要简洁、准确
```

### 7.2 Prompt 示例

```
【系统角色】
你是一个数据分析助手，可以根据提供的业务数据回答用户问题。

【用户查询条件】
{"startDate":"2024-01-01","endDate":"2024-01-31"}

【相关业务数据】
[
  {"orderNo":"ORD202401001","customer":"张三","amount":1500.00,"status":"已完成","createdAt":"2024-01-15 10:30:00"},
  {"orderNo":"ORD202401002","customer":"李四","amount":2800.00,"status":"处理中","createdAt":"2024-01-16 14:20:00"},
  {"orderNo":"ORD202401003","customer":"王五","amount":999.00,"status":"已取消","createdAt":"2024-01-17 09:15:00"},
  {"orderNo":"ORD202401004","customer":"张三","amount":5600.00,"status":"已完成","createdAt":"2024-01-18 16:45:00"},
  {"orderNo":"ORD202401005","customer":"赵六","amount":3200.00,"status":"已完成","createdAt":"2024-01-19 11:00:00"}
]

【用户问题】
帮我分析下本月订单

【回答要求】
1. 基于以上业务数据回答用户问题
2. 如果数据不足或无法回答，请明确告知
3. 回答要简洁、准确
```

---

## 8. 配置说明

### 8.1 后端配置文件

**application.yml**:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/webchat?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  ai:
    minimax:
      api-key: ${MINIMAX_API_KEY}
      base-url: https://api.minimaxi.chat/v1

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.webchat.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 8.2 环境变量

| 变量名             | 说明              | 示例        |
|-----------------|-----------------|-----------|
| MINIMAX_API_KEY | MiniMax API Key | sb-xxxxxx |

---

## 9. 部署说明

### 9.1 后端部署

```bash
# 1. 打包
mvn clean package -DskipTests

# 2. 运行
java -jar target/webchat-ai-0.0.1-SNAPSHOT.jar
```

### 9.2 前端部署

```bash
# 1. 安装依赖
npm install

# 2. 开发模式
npm run dev

# 3. 生产构建
npm run build
```

---

## 10. 项目目录结构

### 10.1 后端完整结构

```
webchat-ai/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/webchat/
│   │   │   ├── WebChatApplication.java
│   │   │   ├── config/
│   │   │   │   └── SpringAIConfig.java
│   │   │   ├── controller/
│   │   │   │   └── ChatController.java
│   │   │   ├── service/
│   │   │   │   ├── ChatService.java
│   │   │   │   ├── ChatServiceImpl.java
│   │   │   │   ├── BusinessDataService.java
│   │   │   │   └── PromptBuilder.java
│   │   │   ├── mapper/
│   │   │   │   ├── OrderMapper.java
│   │   │   │   ├── ChatHistoryMapper.java
│   │   │   │   └── AiConfigMapper.java
│   │   │   ├── entity/
│   │   │   │   ├── Order.java
│   │   │   │   ├── ChatHistory.java
│   │   │   │   └── AiConfig.java
│   │   │   ├── dto/
│   │   │   │   ├── ChatRequest.java
│   │   │   │   ├── ChatResponse.java
│   │   │   │   └── ChatHistoryDTO.java
│   │   │   └── util/
│   │   │       └── JsonUtil.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── mapper/
│   │           ├── OrderMapper.xml
│   │           ├── ChatHistoryMapper.xml
│   │           └── AiConfigMapper.xml
│   └── test/
│       └── java/com/example/webchat/
│           └── service/
│               └── ChatServiceTest.java
└── docs/
    └── sql/
        └── init.sql
```

### 10.2 前端完整结构

```
webchat-ui/
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── postcss.config.js
├── index.html
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── index.css
│   ├── components/
│   │   ├── ChatInput.tsx
│   │   ├── ChatMessage.tsx
│   │   ├── QueryConditionPanel.tsx
│   │   └── ChatContainer.tsx
│   ├── hooks/
│   │   └── useChat.ts
│   ├── store/
│   │   └── chatStore.ts
│   ├── api/
│   │   └── chatApi.ts
│   └── types/
│       └── chat.ts
└── public/
```

---

## 11. 实现要点

### 11.1 流式响应实现

后端使用 `StreamingResponseBody` 实现 SSE 流式输出：

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public StreamingResponseBody streamChat(@RequestBody ChatRequest request) {
    return outputStream -> {
        // 调用 AI 服务并流式写入 outputStream
    };
}
```

前端使用 `EventSource` 或 `fetch` + `ReadableStream` 接收：

```typescript
const response = await fetch('/api/chat/stream', {
  method: 'POST',
  body: JSON.stringify(request),
});
const reader = response.body.getReader();
```

### 11.2 会话管理

- 前端生成 `sessionId`（UUID），用于关联同一会话的所有消息
- 后端根据 `sessionId` 查询历史对话
- 新建会话时，前端生成新的 `sessionId`

### 11.3 错误处理

- AI 服务不可用时，返回友好错误提示
- 数据库连接失败时，记录日志并返回错误信息
- 前端显示加载状态和错误状态

---

## 12. 后续扩展建议

1. **认证鉴权**: 添加用户登录、Token 认证
2. **多模型切换**: 支持 OpenAI、Claude 等多模型切换
3. **文件上传**: 支持上传 Excel、PDF 等文件作为上下文
4. **插件系统**: 支持自定义工具调用扩展
5. **缓存优化**: 添加 Redis 缓存热点数据
6. **监控告警**: 添加 Prometheus 指标监控

---

## 13. 变更记录

| 日期         | 版本  | 变更内容 | 作者 |
|------------|-----|------|----|
| 2026-04-24 | 1.0 | 初始版本 | -  |

---

*文档生成时间: 2026-04-24*
