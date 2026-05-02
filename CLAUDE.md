# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a learning/demo repository exploring AI Agent patterns: how an AI agent works (`001-how-a-agent-work`) and tool calling (`002-how-to-tool-calling`). Each subdirectory contains its own `webchat-ai/` (Spring Boot backend) and `webchat-ui/` (React frontend). The active development target is `002-how-to-tool-calling/`.

## Build & Run Commands

### Backend (`002-how-to-tool-calling/webchat-ai/`)

```bash
cd webchat-ai
./mvnw spring-boot:run          # start backend on port 8080
./mvnw clean compile             # compile only
./mvnw test                      # run tests
```

Requires environment variable `GLM_API_KEY` for the LLM API key. MySQL must be running with the `webchat` database initialized.

### Frontend (`002-how-to-tool-calling/webchat-ui/`)

```bash
cd webchat-ui
pnpm install                     # install dependencies
pnpm dev                         # dev server on port 5173
pnpm build                       # production build (tsc + vite)
```

Vite dev proxy forwards `/api` to `localhost:8080`.

## Architecture

### Backend (`com.piggsoft.webchat`)

**Tech stack**: Spring Boot 3.2.5, Spring AI 1.1.4, WebFlux (reactive), MyBatis 3.0.3, MySQL, Java 17.

**AI Provider**: GLM-4.5-Air via OpenAI-compatible API at `open.bigmodel.cn`. Configured in `application.yml` under `spring.ai.openai.*`. The completions endpoint is `/chat/completions`.

**Two parallel tool-calling approaches** both exposed as streaming SSE endpoints:

| Endpoint | Implementation | Tool Registration |
|---|---|---|
| `POST /api/chat/stream` | `ChatServiceImpl.java` | `@Tool` annotation on `BusinessDataService.queryOrders()` |
| `POST /api/chat/stream/custom` | `CustomToolCallServiceImpl.java` | Manual JSON Schema in `buildToolDefinitions()`, raw `WebClient` HTTP calls |

Both use `Flux<String>` return types (WebFlux reactive streaming). Spring WebFlux auto-wraps each String element as `data: <string>\n\n` for SSE.

**Key services**:
- `PromptBuilder.java` — loads system prompt from `ai_config` DB table, builds user messages
- `BusinessDataService.java` / `BusinessDataServiceImpl.java` — `@Tool` annotated method for Spring AI; also reused by custom implementation via `OrderMapper`
- `PrettyLoggerAdvisor.java` — Spring AI call/stream advisor for logging (used only by Spring AI path)

**Database**: Three tables — `chat_history` (session-based conversation), `ai_config` (key-value config like system prompt), `order` (business data for tool queries with fields: `order_no`, `customer`, `amount`, `status`, `created_at`).

### Frontend (`002-how-to-tool-calling/webchat-ui/`)

**Tech stack**: React 18, TypeScript, Vite, Tailwind CSS 3, Zustand.

**Key files**:
- `src/api/chatApi.ts` — SSE consumption via `@microsoft/fetch-event-source`. Selects endpoint based on `ToolCallMode` (`'spring'` or `'custom'`)
- `src/store/chatStore.ts` — Zustand store. `updateLastMessage` concats incoming chunks to the last assistant message's `content`
- `src/components/ChatMessage.tsx` — renders user messages as plain text, assistant messages as Markdown via `react-markdown` + `remark-gfm` + `remark-breaks`
- `src/components/ChatContainer.tsx` — main chat UI with mode toggle

**State flow**: `handleSend` → `addMessage(user)` → `addMessage(assistant empty)` → `sendChatStream(onChunk=updateLastMessage)` → streaming appends content by mutation.

### CustomToolCallServiceImpl Data Flow

This service implements a **two-phase** tool calling pattern without Spring AI:

1. **Phase 1 (non-streaming)**: Send system + user + tool definitions to LLM with `stream: false`. Check `finish_reason`:
   - `"tool_calls"` → extract assistant text content (shown immediately to user), execute tools locally via `OrderMapper.query()`, add tool results as `role: "tool"` messages, then proceed to Phase 2
   - `"stop"` → add assistant message to context, proceed to Phase 2
2. **Phase 2 (streaming)**: Send the full message history with `stream: true`. Parse SSE response — handles both `data: {...}` and bare `{...}` lines (vendor-dependent). Extract `choices[0].delta.content` from each JSON chunk.

**Logging**: All logs prefixed with `[Custom]`. Key log points: request bodies (no API keys), non-streaming full response, streaming chunks, tool execution params/results.

## SSE Format Variance

Different LLM providers may not include the `data:` prefix in SSE responses. The `callStreaming` parser handles both:
- Standard: `data: {"choices":[...]}` → strips prefix
- Bare JSON: `{"choices":[...]}` → passes through unchanged
- Sentinel: `data: [DONE]` or `[DONE]` → filtered out
