import {fetchEventSource, EventSourceMessage} from '@microsoft/fetch-event-source'
import {ApiResponse, ChatHistory, ChatRequest, ToolCallMode} from '../types/chat'

const API_BASE = '/api/chat'

export async function sendChatStream(
    request: ChatRequest,
    mode: ToolCallMode,
    onChunk: (content: string) => void,
    onDone: () => void,
    onError: (error: Error) => void
): Promise<void> {
    const controller = new AbortController();

    // 根据模式选择不同的端点
    const endpoint = mode === 'custom' ? `${API_BASE}/stream/custom` : `${API_BASE}/stream`

    try {
        await fetchEventSource(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request),
            signal: controller.signal,
            openWhenHidden: true,

            async onmessage(event: EventSourceMessage) {
                // 1. 收到结束标志，执行回调，但不要立刻 abort()
                if (event.data === '[DONE]') {
                    onDone();
                    // 这里不调用 abort()，让后端流自然结束触发 onclose
                    return;
                }

                if (event.data) {
                    onChunk(event.data);
                }
            },

            onclose() {
                // 2. 只有在这里触发的才是“正常结束”
                console.log("服务器已正常关闭流");
                onDone();
                // 如果此时连接还没断开，可以作为清理逻辑调用
                // controller.abort();
            },

            onerror(error: Error) {
                // 如果是因为我们手动切换页面导致的 abort，不报错
                if (controller.signal.aborted) return;
                onError(error);
                throw error; // 必须抛出以停止自动重连
            }
        });
    } catch (error: any) {
        if (error.name !== 'AbortError') {
            onError(error);
        }
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
