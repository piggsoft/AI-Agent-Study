import {useState, useEffect} from 'react'
import ChatContainer from './components/ChatContainer'
import {useChatStore} from './store/chatStore'
import {ToolCallMode} from './types/chat'

interface ChatSession {
    id: string
    title: string
    timestamp: Date
}

function App() {
    const {toolCallMode, setToolCallMode} = useChatStore()
    const [darkMode, setDarkMode] = useState(() => {
        const saved = localStorage.getItem('darkMode')
        return saved ? JSON.parse(saved) : window.matchMedia('(prefers-color-scheme: dark)').matches
    })
    const [sidebarOpen, setSidebarOpen] = useState(false)
    const [sessions, setSessions] = useState<ChatSession[]>(() => {
        const saved = localStorage.getItem('chatSessions')
        return saved ? JSON.parse(saved) : []
    })
    const [currentSessionId, setCurrentSessionId] = useState<string | null>(null)

    useEffect(() => {
        if (darkMode) {
            document.documentElement.classList.add('dark')
        } else {
            document.documentElement.classList.remove('dark')
        }
        localStorage.setItem('darkMode', JSON.stringify(darkMode))
    }, [darkMode])

    useEffect(() => {
        localStorage.setItem('chatSessions', JSON.stringify(sessions))
    }, [sessions])

    const createNewSession = () => {
        const newSession: ChatSession = {
            id: crypto.randomUUID(),
            title: '新对话',
            timestamp: new Date()
        }
        setSessions([newSession, ...sessions])
        setCurrentSessionId(newSession.id)
        setSidebarOpen(false)
    }

    return (
        <div className={`min-h-screen ${darkMode ? 'dark' : ''}`}>
            {/* 背景装饰 */}
            <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
                <div
                    className={`absolute top-0 -right-40 w-96 h-96 rounded-full blur-3xl opacity-20 ${
                        darkMode ? 'bg-emerald-500' : 'bg-emerald-400'
                    }`}/>
                <div
                    className={`absolute -bottom-40 -left-40 w-96 h-96 rounded-full blur-3xl opacity-20 ${
                        darkMode ? 'bg-cyan-500' : 'bg-cyan-400'
                    }`}/>
            </div>

            <div className="flex h-screen">
                {/* Mobile Overlay */}
                {sidebarOpen && (
                    <div
                        className="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-sm"
                        onClick={() => setSidebarOpen(false)}
                    />
                )}

                {/* Sidebar */}
                <aside className={`
          fixed lg:relative z-50 h-full w-72
          ${darkMode ? 'bg-gray-900/80' : 'bg-white/80'} 
          backdrop-blur-xl border-r
          ${darkMode ? 'border-gray-800' : 'border-gray-200'}
          transform transition-all duration-300 ease-out
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
          flex flex-col shadow-2xl lg:shadow-none
        `}>
                    {/* Logo */}
                    <div className="p-5 border-b border-gray-200/50 dark:border-gray-700/50">
                        <div className="flex items-center gap-3">
                            <div
                                className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 via-cyan-500 to-teal-500 flex items-center justify-center shadow-lg glow text-xl">
                                🤖
                            </div>
                            <div>
                                <h1 className="font-bold text-gray-900 dark:text-white text-lg">AI 助手</h1>
                                <p className="text-xs text-gray-500 dark:text-gray-400">智能对话平台</p>
                            </div>
                        </div>
                    </div>

                    {/* Sidebar Header */}
                    <div className="p-4">
                        <button
                            onClick={createNewSession}
                            className="w-full flex items-center justify-center gap-2 px-4 py-3
                bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-600 hover:to-cyan-600
                text-white rounded-xl transition-all duration-200 shadow-lg hover:shadow-xl
                hover:scale-[1.02] active:scale-[0.98]"
                        >
                            <i className="fa-solid fa-plus"/>
                            新建对话
                        </button>
                    </div>

                    {/* Session List */}
                    <div className="flex-1 overflow-y-auto py-2 px-3">
                        <div
                            className="text-xs font-medium text-gray-400 dark:text-gray-500 uppercase tracking-wider px-2 py-2">
                            历史记录
                        </div>
                        {sessions.length === 0 ? (
                            <div className="text-sm text-gray-400 dark:text-gray-500 text-center py-8 px-3">
                                <i className="fa-regular fa-clock text-2xl mb-2 block opacity-50"/>
                                暂无历史对话
                            </div>
                        ) : (
                            sessions.map((session, index) => (
                                <button
                                    key={session.id}
                                    onClick={() => {
                                        setCurrentSessionId(session.id)
                                        setSidebarOpen(false)
                                    }}
                                    className={`w-full text-left px-3 py-2.5 rounded-lg mb-1 transition-all duration-200 truncate
                    ${currentSessionId === session.id
                                        ? 'bg-gradient-to-r from-emerald-500/20 to-cyan-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                                        : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                                    }`}
                                    style={{animationDelay: `${index * 50}ms`}}
                                >
                                    <div className="flex items-center gap-2">
                                        <i className={`fa-regular ${currentSessionId === session.id ? 'fa-comment-dots' : 'fa-comment'}`}/>
                                        <span className="truncate">{session.title}</span>
                                    </div>
                                </button>
                            ))
                        )}
                    </div>

                    {/* Tool Call Mode - Sidebar */}
                    <div className="p-4 border-t border-gray-200/50 dark:border-gray-700/50">
                        <div
                            className="text-xs font-medium text-gray-400 dark:text-gray-500 uppercase tracking-wider px-2 py-1">
                            Tool Call 方式
                        </div>
                        <div className="flex rounded-xl bg-gray-100 dark:bg-gray-800 p-1 mt-1">
                            {(['spring', 'custom'] as ToolCallMode[]).map((mode) => (
                                <button
                                    key={mode}
                                    onClick={() => setToolCallMode(mode)}
                                    className={`flex-1 px-3 py-2 rounded-lg text-xs font-medium transition-all duration-200
                    ${toolCallMode === mode
                                        ? 'bg-white dark:bg-gray-700 text-emerald-600 dark:text-emerald-400 shadow-sm'
                                        : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                                    }`}
                                >
                                    {mode === 'spring' ? 'Spring AI' : 'Custom'}
                                </button>
                            ))}
                        </div>
                        <p className="text-[10px] text-gray-400 dark:text-gray-500 mt-1.5 px-2 leading-relaxed">
                            {toolCallMode === 'spring'
                                ? '使用 @Tool 注解自动完成 Tool Call'
                                : '使用 WebClient 手工实现 Tool Call'}
                        </p>
                    </div>

                    {/* Sidebar Footer */}
                    <div className="p-4 border-t border-gray-200/50 dark:border-gray-700/50">
                        <div className="flex items-center gap-3 px-3 py-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                            <div
                                className="w-9 h-9 rounded-full bg-gradient-to-br from-emerald-400 to-cyan-500 flex items-center justify-center text-white text-lg shadow-md">
                                👤
                            </div>
                            <div className="flex-1 min-w-0">
                                <div className="text-sm font-medium text-gray-900 dark:text-white truncate">访客用户</div>
                                <div className="text-xs text-emerald-600 dark:text-emerald-400 truncate flex items-center gap-1">
                                    <i className="fa-solid fa-circle text-[6px]"/>
                                    在线
                                </div>
                            </div>
                        </div>
                    </div>
                </aside>

                {/* Main Content */}
                <main className="flex-1 flex flex-col min-w-0">
                    {/* Top Bar (Mobile) */}
                    <div
                        className="lg:hidden flex items-center gap-3 p-4 border-b border-gray-200/50 dark:border-gray-800/50 bg-white/50 dark:bg-gray-900/50 backdrop-blur-xl">
                        <button
                            onClick={() => setSidebarOpen(true)}
                            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                        >
                            <i className="fa-solid fa-bars text-gray-600 dark:text-gray-300"/>
                        </button>
                        <div className="flex items-center gap-2">
                            <div
                                className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500 to-cyan-500 flex items-center justify-center text-lg">
                                🤖
                            </div>
                            <span className="font-semibold text-gray-900 dark:text-white">AI 助手</span>
                        </div>
                    </div>

                    <ChatContainer/>

                    {/* Tool Call Mode Toggle (Desktop) */}
                    <div
                        className="hidden lg:flex fixed bottom-24 right-6 z-50 flex-col gap-1
              bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl border border-gray-200/50 dark:border-gray-700/50
              rounded-2xl shadow-xl p-1.5"
                    >
                        <div className="text-[10px] font-medium text-gray-400 dark:text-gray-500 uppercase tracking-wider px-2 py-1 text-center">
                            Tool Call
                        </div>
                        {(['spring', 'custom'] as ToolCallMode[]).map((mode) => (
                            <button
                                key={mode}
                                onClick={() => setToolCallMode(mode)}
                                className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-medium transition-all duration-200 whitespace-nowrap
                  ${toolCallMode === mode
                                    ? 'bg-gradient-to-r from-emerald-500 to-cyan-500 text-white shadow-md'
                                    : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                                }`}
                                title={mode === 'spring' ? 'Spring AI @Tool 注解方式' : 'WebClient 手写方式'}
                            >
                                <span className={`w-2 h-2 rounded-full ${toolCallMode === mode ? 'bg-white' : 'bg-gray-300 dark:bg-gray-600'}`}/>
                                {mode === 'spring' ? 'Spring AI' : 'Custom'}
                            </button>
                        ))}
                    </div>

                    {/* Theme Toggle (Desktop) */}
                    <button
                        onClick={() => setDarkMode(!darkMode)}
                        className="hidden lg:flex fixed bottom-6 right-6 p-3.5 rounded-2xl
              bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl border border-gray-200/50 dark:border-gray-700/50
              shadow-xl hover:shadow-2xl transition-all duration-300 z-50
              hover:scale-110 active:scale-95 group"
                        title={darkMode ? '切换到浅色模式' : '切换到深色模式'}
                    >
                        {darkMode ? (
                            <i className="fa-solid fa-sun text-yellow-500 group-hover:rotate-45 transition-transform duration-300"/>
                        ) : (
                            <i className="fa-solid fa-moon text-gray-600 group-hover:-rotate-12 transition-transform duration-300"/>
                        )}
                    </button>
                </main>
            </div>
        </div>
    )
}

export default App
