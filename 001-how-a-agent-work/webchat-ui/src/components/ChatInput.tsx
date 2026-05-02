import {useState, FormEvent, useRef, useEffect} from 'react'

interface ChatInputProps {
    onSend: (content: string) => void
    disabled?: boolean
}

export default function ChatInput({onSend, disabled}: ChatInputProps) {
    const [input, setInput] = useState('')
    const textareaRef = useRef<HTMLTextAreaElement>(null)

    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto'
            textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 200) + 'px'
        }
    }, [input])

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault()
        if (input.trim() && !disabled) {
            onSend(input.trim())
            setInput('')
            if (textareaRef.current) {
                textareaRef.current.style.height = 'auto'
            }
        }
    }

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            handleSubmit(e)
        }
    }

    const canSend = input.trim().length > 0 && !disabled

    return (
        <form onSubmit={handleSubmit} className="relative">
            {/* Input Container with glow effect */}
            <div className={`
        relative flex items-end rounded-2xl border transition-all duration-300
        bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl
        border-gray-200/80 dark:border-gray-700/50
        focus-within:border-emerald-500/50 dark:focus-within:border-emerald-500/50
        focus-within:shadow-lg focus-within:shadow-emerald-500/10
        ${disabled ? 'opacity-60' : ''}
      `}>
                <textarea
                    ref={textareaRef}
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    disabled={disabled}
                    rows={1}
                    placeholder={disabled ? 'AI 正在思考中...' : '输入消息，Shift+Enter 换行，Enter 发送'}
                    className="flex-1 px-5 py-4 bg-transparent resize-none focus:outline-none 
                   text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 
                   text-sm sm:text-base"
                    style={{maxHeight: '200px'}}
                />

                {/* Character count */}
                {input.length > 0 && (
                    <div className="absolute -top-6 right-2 text-xs text-gray-400 dark:text-gray-500">
                        {input.length} 字符
                    </div>
                )}

                <button
                    type="submit"
                    disabled={!canSend}
                    className={`
            m-1.5 p-3 rounded-xl transition-all duration-300 flex items-center justify-center
            ${canSend
                        ? 'bg-gradient-to-br from-emerald-500 to-cyan-500 hover:from-emerald-600 hover:to-cyan-600 text-white shadow-lg hover:shadow-xl hover:scale-105 active:scale-95'
                        : 'bg-gray-200 dark:bg-gray-700 text-gray-400 dark:text-gray-500 cursor-not-allowed'
                    }
          `}
                >
                    {disabled ? (
                        <span className="text-sm animate-spin">🔄</span>
                    ) : (
                        <span className="text-sm">➤</span>
                    )}
                </button>
            </div>

            {/* Footer hint */}
            <div className="text-center text-xs text-gray-400 dark:text-gray-500 mt-3 flex items-center justify-center gap-2">
                <span>🛡️</span>
                AI 助手可能会产生不准确的信息，请注意辨别
            </div>
        </form>
    )
}
