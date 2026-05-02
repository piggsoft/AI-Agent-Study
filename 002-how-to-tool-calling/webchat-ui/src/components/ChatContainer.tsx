import {useEffect, useRef} from 'react'
import {useChatStore} from '../store/chatStore'
import {sendChatStream} from '../api/chatApi'
import {Message} from '../types/chat'
import ChatMessage from './ChatMessage'
import ChatInput from './ChatInput'

export default function ChatContainer() {
    const {
        sessionId,
        messages,
        isLoading,
        queryConditions,
        toolCallMode,
        addMessage,
        updateLastMessage,
        setLoading
    } = useChatStore()

    const messagesEndRef = useRef<HTMLDivElement>(null)
    const containerRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({behavior: 'smooth'})
    }, [messages])

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

        const assistantMessage: Message = {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: '',
            timestamp: new Date(),
        }
        addMessage(assistantMessage)
        setLoading(true)

        await sendChatStream(
            {
                sessionId,
                message: content,
                queryConditions: Object.keys(queryConditions).length > 0 ? queryConditions : undefined,
            },
            toolCallMode,
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
        <div className="flex flex-col h-full">
            {/* Messages Area */}
            <div
                ref={containerRef}
                className="flex-1 overflow-y-auto scrollbar-thin"
            >
                {messages.length === 0 ? (
                    /* Empty State - Premium Demo Style */
                    <div className="h-full flex flex-col items-center justify-center px-4 relative">
                        {/* Animated background decoration */}
                        <div className="absolute inset-0 overflow-hidden pointer-events-none">
                            <div
                                className="absolute top-1/4 left-1/4 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl animate-float"/>
                            <div
                                className="absolute bottom-1/4 right-1/4 w-64 h-64 bg-cyan-500/10 rounded-full blur-3xl animate-float"
                                style={{animationDelay: '1.5s'}}/>
                        </div>

                        {/* Hero Icon */}
                        <div
                            className="relative mb-8 animate-float">
                            <div
                                className="absolute inset-0 bg-gradient-to-r from-emerald-500 to-cyan-500 rounded-full blur-xl opacity-30"/>
                            <div
                                className="relative w-20 h-20 rounded-2xl bg-gradient-to-br from-emerald-500 via-cyan-500 to-teal-500 flex items-center justify-center shadow-2xl glow text-4xl">
                                🧠
                            </div>
                        </div>

                        {/* Title */}
                        <h2 className="relative text-3xl font-bold text-gray-800 dark:text-gray-100 mb-3 text-center">
                            有什么可以帮助你的？
                        </h2>
                        <p className="relative text-gray-500 dark:text-gray-400 text-center max-w-lg mb-10 leading-relaxed">
                            基于先进 AI 大模型，智能对话、代码编写、数据分析、创意写作，让工作更高效
                        </p>

                        {/* Quick Examples - Premium Cards */}
                        <div
                            className="relative grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-3xl w-full">
                            {[
                                {
                                    emoji: '💡',
                                    text: '解释量子计算的基本原理',
                                    gradient: 'from-amber-500/20 to-orange-500/20',
                                    border: 'border-amber-500/30',
                                    hover: 'hover:border-amber-500/60',
                                },
                                {
                                    emoji: '📝',
                                    text: '帮我写一封辞职邮件',
                                    gradient: 'from-blue-500/20 to-indigo-500/20',
                                    border: 'border-blue-500/30',
                                    hover: 'hover:border-blue-500/60',
                                },
                                {
                                    emoji: '💻',
                                    text: '用 Python 写快速排序',
                                    gradient: 'from-emerald-500/20 to-teal-500/20',
                                    border: 'border-emerald-500/30',
                                    hover: 'hover:border-emerald-500/60',
                                },
                                {
                                    emoji: '📊',
                                    text: '分析数据趋势并可视化',
                                    gradient: 'from-purple-500/20 to-pink-500/20',
                                    border: 'border-purple-500/30',
                                    hover: 'hover:border-purple-500/60',
                                },
                            ].map((item, index) => (
                                <button
                                    key={index}
                                    onClick={() => handleSend(item.text)}
                                    className={`
                    relative p-5 rounded-2xl border backdrop-blur-sm
                    bg-gradient-to-br ${item.gradient} ${item.border} ${item.hover}
                    hover:shadow-xl hover:scale-[1.02] hover:-translate-y-1
                    transition-all duration-300 text-left group
                    animate-slideIn
                  `}
                                    style={{animationDelay: `${index * 100}ms`}}
                                >
                                    <div className="flex items-center gap-4">
                                        <div
                                            className="w-10 h-10 rounded-xl bg-white/80 dark:bg-gray-800/80 flex items-center justify-center text-xl shadow-md group-hover:scale-110 transition-transform duration-300">
                                            {item.emoji}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-sm font-medium text-gray-700 dark:text-gray-200 group-hover:text-gray-900 dark:group-hover:text-white">
                                                {item.text}
                                            </p>
                                        </div>
                                        <span className="text-gray-400 group-hover:text-gray-600 dark:group-hover:text-gray-300 group-hover:translate-x-1 transition-all duration-300">→</span>
                                    </div>
                                </button>
                            ))}
                        </div>

                        {/* Feature Tags */}
                        <div
                            className="relative flex flex-wrap justify-center gap-3 mt-10 max-w-2xl">
                            {['智能问答', '代码助手', '文案创作', '数据分析', '翻译助手'].map((tag, i) => (
                                <span
                                    key={i}
                                    className="px-4 py-1.5 rounded-full text-xs font-medium
                        bg-gray-100 dark:bg-gray-800/50 text-gray-600 dark:text-gray-400
                        border border-gray-200 dark:border-gray-700/50
                        hover:border-emerald-500/50 hover:text-emerald-600 dark:hover:text-emerald-400
                        transition-all duration-300 cursor-default"
                                    style={{animationDelay: `${i * 50 + 400}ms`}}
                                >
                                    {tag}
                                </span>
                            ))}
                        </div>
                    </div>
                ) : (
                    <div className="max-w-3xl mx-auto px-4 py-6">
                        {messages.map((message, index) => (
                            <div key={message.id} className="animate-slideIn" style={{animationDelay: `${index * 50}ms`}}>
                                <ChatMessage message={message}/>
                            </div>
                        ))}
                        <div ref={messagesEndRef}/>
                    </div>
                )}
            </div>

            {/* Input Area - Bottom Center */}
            <div className="flex-shrink-0 px-4 pb-4 sm:pb-6">
                <div className="max-w-3xl mx-auto">
                    <ChatInput onSend={handleSend} disabled={isLoading}/>
                </div>
            </div>
        </div>
    )
}
