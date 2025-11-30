package cc.unitmesh.devins.idea.toolwindow.codereview

import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.ui.compose.agent.codereview.CodeReviewViewModel
import cc.unitmesh.devins.workspace.DefaultWorkspace
import cc.unitmesh.devins.workspace.Workspace
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

/**
 * ViewModel for Code Review in IntelliJ IDEA plugin.
 *
 * This class extends the common CodeReviewViewModel from mpp-ui,
 * adapting it for the IntelliJ platform by:
 * - Creating a Workspace from IntelliJ Project
 * - Using JewelRenderer for native IntelliJ theme integration
 * - Implementing Disposable for proper resource cleanup
 *
 * All core functionality (git operations, analysis, plan generation, fix generation)
 * is inherited from the base CodeReviewViewModel.
 */
class IdeaCodeReviewViewModel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) : CodeReviewViewModel(
    workspace = createWorkspaceFromProject(project)
), Disposable {

    private val logger = Logger.getInstance(IdeaCodeReviewViewModel::class.java)

    // JewelRenderer for IntelliJ native theme
    val jewelRenderer = JewelRenderer()

    companion object {
        /**
         * Create a Workspace from an IntelliJ Project
         */
        private fun createWorkspaceFromProject(project: Project): Workspace {
            val projectPath = project.basePath
            val projectName = project.name

            return if (projectPath != null) {
                DefaultWorkspace.create(projectName, projectPath)
            } else {
                DefaultWorkspace.createEmpty(projectName)
            }
        }
    }

    /**
     * Dispose resources when the ViewModel is no longer needed
     */
    override fun dispose() {
        logger.info("Disposing IdeaCodeReviewViewModel")
        // The parent class cleanup will happen when the scope is cancelled
    }
}
