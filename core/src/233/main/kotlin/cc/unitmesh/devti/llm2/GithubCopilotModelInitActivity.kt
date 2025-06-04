package cc.unitmesh.devti.llm2

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

/**
 * 启动时初始化 GitHub Copilot 模型的 Activity
 * 仅适用于 IDEA 233+ 版本
 */
class GithubCopilotModelInitActivity : ProjectActivity {
    @RequiresBackgroundThread
    override suspend fun execute(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) return

        // 只有当用户已配置 GitHub Copilot 时才进行初始化
        if (GithubCopilotDetector.isGithubCopilotConfigured()) {
            // 获取服务实例并初始化
            val manager = GithubCopilotManager.getInstance()
            manager.initialize()
        }
    }
}
