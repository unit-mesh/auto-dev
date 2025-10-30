package cc.unitmesh.devins.completion

import cc.unitmesh.devins.completion.providers.AgentCompletionProvider
import cc.unitmesh.devins.completion.providers.FilePathCompletionProvider
import cc.unitmesh.devins.completion.providers.SpecKitCommandCompletionProvider
import cc.unitmesh.devins.completion.providers.ToolBasedCommandCompletionProvider
import cc.unitmesh.devins.completion.providers.VariableCompletionProvider
import cc.unitmesh.devins.filesystem.ProjectFileSystem

/**
 * 补全管理器 - 根据上下文选择合适的 Provider
 * 现在通过 Workspace 系统进行管理，但保持向后兼容性
 */
class CompletionManager(fileSystem: ProjectFileSystem? = null) {
    private val specKitProvider = SpecKitCommandCompletionProvider(fileSystem)

    private val providers = mapOf(
        CompletionTriggerType.AGENT to AgentCompletionProvider(),
        CompletionTriggerType.COMMAND to ToolBasedCommandCompletionProvider(),
        CompletionTriggerType.VARIABLE to VariableCompletionProvider(),
        CompletionTriggerType.COMMAND_VALUE to FilePathCompletionProvider()
    )

    fun getCompletions(context: CompletionContext): List<CompletionItem> {
        val provider = providers[context.triggerType] ?: return emptyList()
        val baseCompletions = provider.getCompletions(context)

        // 对于 COMMAND 类型，同时包含 SpecKit 命令
        return if (context.triggerType == CompletionTriggerType.COMMAND) {
            baseCompletions + specKitProvider.getCompletions(context)
        } else {
            baseCompletions
        }
    }

    /**
     * 刷新 SpecKit 命令（当项目路径改变时调用）
     */
    fun refreshSpecKitCommands() {
        specKitProvider.refresh()
    }

    /**
     * 获取所有支持的触发类型
     */
    fun getSupportedTriggerTypes(): Set<CompletionTriggerType> {
        return providers.keys
    }

    /**
     * 检查是否支持指定的触发类型
     */
    fun supports(triggerType: CompletionTriggerType): Boolean {
        return triggerType in providers.keys
    }
}