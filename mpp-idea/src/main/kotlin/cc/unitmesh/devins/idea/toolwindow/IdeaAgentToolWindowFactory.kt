package cc.unitmesh.devins.idea.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import cc.unitmesh.devins.idea.services.CoroutineScopeHolder
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.foundation.JewelFlags

/**
 * Factory for creating the Agent ToolWindow with tab-based navigation.
 *
 * Features:
 * - Tab-based agent type switching (similar to TopBarMenuDesktop from mpp-ui)
 * - Agentic: Full coding agent with file operations
 * - Review: Code review and analysis
 * - Knowledge: Document reading and Q&A
 * - Remote: Connect to remote mpp-server
 *
 * Uses Jewel theme for native IntelliJ IDEA integration (2025.2+).
 */
class IdeaAgentToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().info("IdeaAgentToolWindowFactory initialized - Agent Tabs UI for IntelliJ IDEA 252+")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Enable custom popup rendering to use JBPopup instead of default Compose implementation
        // This fixes z-index issues when Compose Popup is used with SwingPanel (e.g., EditorTextField)
        JewelFlags.useCustomPopupRenderer = true

        createAgentPanel(project, toolWindow)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    private fun createAgentPanel(project: Project, toolWindow: ToolWindow) {
        val coroutineScope = project.service<CoroutineScopeHolder>()
            .createScope("IdeaAgentViewModel")

        val viewModel = IdeaAgentViewModel(project, coroutineScope)
        Disposer.register(toolWindow.disposable, viewModel)

        toolWindow.addComposeTab("Agent") {
            IdeaAgentApp(viewModel, project, coroutineScope)
        }
    }
}

