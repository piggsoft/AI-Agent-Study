import {useState} from 'react'
import {useChatStore} from '../store/chatStore'
import {Filter, X} from 'lucide-react'

export default function QueryConditionPanel() {
    const {queryConditions, setQueryConditions} = useChatStore()
    const [isExpanded, setIsExpanded] = useState(false)
    const [localConditions, setLocalConditions] = useState({
        startDate: queryConditions.startDate || '',
        endDate: queryConditions.endDate || '',
    })

    const handleApply = () => {
        const conditions: Record<string, string> = {}
        if (localConditions.startDate) conditions.startDate = localConditions.startDate
        if (localConditions.endDate) conditions.endDate = localConditions.endDate
        setQueryConditions(conditions)
        setIsExpanded(false)
    }

    const handleClear = () => {
        setQueryConditions({})
        setLocalConditions({startDate: '', endDate: ''})
    }

    const hasConditions = Object.keys(queryConditions).length > 0

    return (
        <div className="border-b bg-gray-50">
            <button
                onClick={() => setIsExpanded(!isExpanded)}
                className="w-full px-6 py-3 flex items-center justify-between text-gray-700 hover:bg-gray-100 transition-colors"
            >
                <div className="flex items-center gap-2">
                    <Filter size={18}/>
                    <span className="font-medium">查询条件</span>
                    {hasConditions && (
                        <span className="px-2 py-0.5 bg-blue-100 text-blue-700 text-xs rounded-full">
              已设置
            </span>
                    )}
                </div>
                <span className="text-sm">{isExpanded ? '收起' : '展开'}</span>
            </button>

            {isExpanded && (
                <div className="px-6 pb-4 space-y-4">
                    <div className="flex gap-4">
                        <div className="flex-1">
                            <label className="block text-sm font-medium text-gray-600 mb-1">
                                开始日期
                            </label>
                            <input
                                type="date"
                                value={localConditions.startDate}
                                onChange={(e) => setLocalConditions({...localConditions, startDate: e.target.value})}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                        <div className="flex-1">
                            <label className="block text-sm font-medium text-gray-600 mb-1">
                                结束日期
                            </label>
                            <input
                                type="date"
                                value={localConditions.endDate}
                                onChange={(e) => setLocalConditions({...localConditions, endDate: e.target.value})}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                    </div>

                    <div className="flex gap-2">
                        <button
                            onClick={handleApply}
                            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                        >
                            应用条件
                        </button>
                        <button
                            onClick={handleClear}
                            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors flex items-center gap-1"
                        >
                            <X size={16}/>
                            清除
                        </button>
                    </div>

                    {hasConditions && (
                        <div className="text-sm text-gray-500">
                            当前条件：{JSON.stringify(queryConditions)}
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}
