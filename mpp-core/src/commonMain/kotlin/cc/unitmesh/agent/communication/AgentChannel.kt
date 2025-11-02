package cc.unitmesh.agent.communication

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Agent 通信通道
 * 实现 Queue Pair 模式，解耦 Agent 和 UI
 * 
 * 参考 Codex 的异步通信设计
 */
class AgentChannel {
    // 提交队列：UI -> Agent
    private val submissionChannel = Channel<AgentSubmission>(capacity = Channel.BUFFERED)

    // 事件队列：Agent -> UI
    private val eventChannel = Channel<AgentEvent>(capacity = Channel.UNLIMITED)

    /**
     * 提交一个操作到 Agent
     */
    suspend fun submit(submission: AgentSubmission) {
        submissionChannel.send(submission)
    }

    /**
     * 发送一个事件到 UI
     */
    suspend fun emit(event: AgentEvent) {
        eventChannel.send(event)
    }

    /**
     * 获取提交流
     */
    fun submissions(): Flow<AgentSubmission> = submissionChannel.receiveAsFlow()

    /**
     * 获取事件流
     */
    fun events(): Flow<AgentEvent> = eventChannel.receiveAsFlow()

    /**
     * 关闭通道
     */
    fun close() {
        submissionChannel.close()
        eventChannel.close()
    }
}

