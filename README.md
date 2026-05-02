# WebChat AI System

一个基于 Spring Boot + React 的智能对话系统，连接 MiniMax 大模型，支持业务数据上下文注入。

## 项目结构

```
webchat-ai/          # 后端 Spring Boot 项目
webchat-ui/          # 前端 React 项目
docs/                # 文档目录
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


💡 为什么你访问大厂的 AI（比如我）没有这个问题？
正如你观察到的，成熟的 AI 产品切到后台是不受影响的。除了上面提到的心跳机制，大厂的终极架构是**“前后端生命周期解耦”**：

你的现状（同步耦合）：HTTP 连接活着，大模型才生成；HTTP 断了，大模型就停止（或结果被丢弃）。

工业级架构（异步解耦）：

前端点击发送，后端立刻创建一个 taskId 返回给前端，然后后端开启独立的后台线程去查数据库和调用 LLM，把结果源源不断写进 Redis 队列。

前端的 SSE 连接只是在“监听”这个 Redis 队列。

如果你切到了 IDEA 导致 SSE 断了，后端的生成线程根本不会停，它继续往 Redis 里写。

当你切回浏览器，前端带着 taskId 重新建立 SSE，把 Redis 里你没看到的剩余内容一股脑拉出来。

总结建议：
针对你目前的单体架构，先使用上面的 “彻底禁用重试 + 后端 Ping 心跳 + 延长超时” 方案。这能解决 90% 以上因为切换窗口导致的异常重启问题。如果未来要做商业级产品，再考虑引入 Redis 队列解耦。