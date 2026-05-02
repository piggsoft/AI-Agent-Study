新建 002-how-to-tool-calling 目录
将 webchat-ui 的代码copy过来，不用包含node_modules
但在页面上加一个切换按钮，一个 spring-tool-calling 一个是 custom-tool-call

新建一个webchat-ai项目和 001-how-a-agent-work 类的类似。只不过有两个大模型交互接口，一个使用springai的方法 @Tool注解完成tool call，一个使用webclient 全部手写一个。

如果有任何不清楚的地方请找我确认，禁止猜测