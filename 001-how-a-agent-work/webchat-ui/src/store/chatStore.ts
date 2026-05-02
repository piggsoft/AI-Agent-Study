import {create} from 'zustand'
import {Message, QueryConditions} from '../types/chat'

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
    setSessionId: (sessionId) => set({sessionId}),
    addMessage: (message) => set((state) => ({
        messages: [...state.messages, message]
    })),
    updateLastMessage: (content) => set((state) => {
        const messages = [...state.messages]
        if (messages.length > 0) {
            messages[messages.length - 1].content += content
        }
        return {messages}
    }),
    setLoading: (isLoading) => set({isLoading}),
    setQueryConditions: (conditions) => set({queryConditions: conditions}),
    clearMessages: () => set({messages: [], sessionId: crypto.randomUUID()}),
}))
