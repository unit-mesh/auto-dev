package cc.unitmesh.devins.idea.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import cc.unitmesh.devins.idea.services.CoroutineScopeHolder
import org.jetbrains.jewel.bridge.addComposeTab

/**
 * Factory for creating the AutoDev Compose ToolWindow.
 *
 * This factory creates a Compose-based UI tool window that uses the Jewel theme
 * for native IntelliJ IDEA integration (2025.2+).
 */
class AutoDevToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().info("AutoDevToolWindowFactory initialized - Compose UI for IntelliJ IDEA 252+")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        createChatPanel(project, toolWindow)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    private fun createChatPanel(project: Project, toolWindow: ToolWindow) {
        val coroutineScope = project.service<CoroutineScopeHolder>()
            .createScope("AutoDevChatViewModel")

        val viewModel = AutoDevChatViewModel(coroutineScope)
        Disposer.register(toolWindow.disposable, viewModel)

        toolWindow.addComposeTab("Chat") {
            AutoDevChatApp(viewModel)
        }
    }
}

