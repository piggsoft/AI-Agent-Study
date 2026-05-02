# WebChat AI Backend

后端 Spring Boot 项目，使用 Spring AI 集成 MiniMax 大模型。

## 技术栈

- Spring Boot 3.2.5
- Spring AI 1.0.0-M6
- MyBatis 3.0.3
- MySQL 8.x

## 配置

在 `src/main/resources/application.yml` 中配置：

```yaml
spring:
  ai:
    minimax:
      api-key: ${MINIMAX_API_KEY}
      base-url: https://api.minimaxi.chat/v1
```

## 运行

```bash
mvn spring-boot:run
```

## API 接口

- `POST /api/chat/stream` - 流式对话
- `GET /api/chat/history/{sessionId}` - 获取历史消息
