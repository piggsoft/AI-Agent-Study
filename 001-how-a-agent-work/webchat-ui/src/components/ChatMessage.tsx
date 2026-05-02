import {Message} from '../types/chat'

interface ChatMessageProps {
    message: Message
}

export default function ChatMessage({message}: ChatMessageProps) {
    const isUser = message.role === 'user'

    return (
        <div className={`flex gap-4 py-5 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
            {/* Avatar */}
            <div className={`flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center shadow-lg ${
                    isUser
                    ? 'bg-gradient-to-br from-gray-400 to-gray-500 dark:from-gray-500 dark:to-gray-600 text-white'
                    : 'bg-gradient-to-br from-emerald-500 via-cyan-500 to-teal-500 text-white glow'
            }`}>
                {isUser ? '👤' : '🤖'}
            </div>

            {/* Message Content */}
            <div className={`flex-1 min-w-0 max-w-2xl ${isUser ? 'text-right' : 'text-left'}`}>
                {/* Query Conditions */}
                {message.queryConditions && !isUser && (
                    <div
                        className="mb-3 px-4 py-3 rounded-xl 
                        bg-gradient-to-r from-emerald-500/10 to-cyan-500/10 
                        dark:from-emerald-500/20 dark:to-cyan-500/20 
                        border border-emerald-500/20 dark:border-emerald-500/30 backdrop-blur-sm">
                        <div className="text-xs text-emerald-600 dark:text-emerald-400 mb-2 font-medium flex items-center gap-1">
                            <i className="fa-solid fa-filter text-[10px]"/>
                            查询条件
                        </div>
                        <div className="flex flex-wrap gap-2">
                            {Object.entries(message.queryConditions).map(([key, value]) => (
                                value && (
                                    <span key={key}
                                          className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs
                                          bg-white/80 dark:bg-gray-800/80 text-gray-700 dark:text-gray-300
                                          border border-gray-200 dark:border-gray-700/50 shadow-sm">
                                        <span
                                            className="text-emerald-600 dark:text-emerald-400 mr-1 font-medium">{key}:</span>
                                        <span className="font-medium">{value}</span>
                                    </span>
                                )
                            ))}
                        </div>
                    </div>
                )}

                {/* Message Bubble */}
                <div
                    className={`inline-block max-w-full ${
                        isUser
                            ? 'bg-[rgb(233,238,246)] dark:bg-gradient-to-br dark:from-gray-600 dark:to-gray-700 text-black dark:text-white rounded-2xl rounded-tr-md px-4 py-3 shadow-lg'
                            : ''
                    }`}>
                    {message.content ? (
                        <div
                            className={`text-sm sm:text-base leading-relaxed whitespace-pre-wrap break-words ${
                                isUser
                                    ? 'text-black'
                                    : 'text-gray-800 dark:text-gray-100'
                            }`}>
                            {message.content}
                        </div>
                    ) : (
                        /* Loading State */
                        <div className="flex items-center gap-1.5 px-2 py-1">
                            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-bounce"/>
                            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-bounce"
                                 style={{animationDelay: '0.1s'}}/>
                            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-bounce"
                                 style={{animationDelay: '0.2s'}}/>
                        </div>
                    )}
                </div>

                {/* Timestamp */}
                {message.timestamp && (
                    <div className={`text-xs text-gray-400 dark:text-gray-500 mt-1.5 ${
                        isUser ? 'text-right' : 'text-left'
                    }`}>
                        {new Date(message.timestamp).toLocaleTimeString('zh-CN', {
                            hour: '2-digit',
                            minute: '2-digit'
                        })}
                    </div>
                )}
            </div>
        </div>
    )
}
