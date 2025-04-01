package cc.unitmesh.terminal.sketch

/**
 * 终端执行命令的状态枚举
 */
enum class TerminalExecutionState {
    /**
     * 准备执行（初始状态）
     */
    READY,

    /**
     * 正在执行中
     */
    EXECUTING,

    /**
     * 执行成功
     */
    SUCCESS,

    /**
     * 执行失败
     */
    FAILED,

    /**
     * 执行被手动终止
     */
    TERMINATED
}
