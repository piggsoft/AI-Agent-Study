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

export type ToolCallMode = 'spring' | 'custom'
