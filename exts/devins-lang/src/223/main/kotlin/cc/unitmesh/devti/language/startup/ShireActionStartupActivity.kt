package cc.unitmesh.devti.language.startup

import cc.unitmesh.devti.custom.team.InteractionType
import cc.unitmesh.devti.language.DevInFileType
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.startup.third.ShireSonarLintToolWindowListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import kotlin.collections.forEach
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch

class ShireActionStartupActivity : StartupActivity  {
    override fun runActivity(project: Project) {
        AutoDevCoroutineScope.scope(project).launch {
            bindingShireActions(project)
        }
    }

    private suspend fun bindingShireActions(project: Project) {
        GlobalShireFileChangesProvider.getInstance().startup(::attachCopyPasteAction)
        val changesProvider = ShireFileChangesProvider.getInstance(project)
        smartReadAction(project) {
            changesProvider.startup { shireConfig, shireFile ->
                attachCopyPasteAction(shireConfig, shireFile)
            }

            obtainShireFiles(project).forEach {
                changesProvider.onUpdated(it)
            }

            attachTerminalAction()
            attachDatabaseAction()
            attachVcsLogAction()
            attachExtensionActions(project)
        }
    }

    private fun attachCopyPasteAction(shireConfig: HobbitHole, shireFile: DevInFile) {
        if (shireConfig.interaction == InteractionType.OnPaste) {
            PasteManagerService.getInstance()
                .registerPasteProcessor(shireConfig, shireFile)
        }
    }

    /**
     * We make terminal plugin optional, so can't add to `TerminalToolwindowActionGroup` the plugin.xml.
     * So we add it manually here, if terminal plugin is not enabled, this action will not be shown.
     */
    private fun attachTerminalAction() {
        val actionManager = ActionManager.getInstance()
        val toolsMenu = actionManager.getAction("TerminalToolwindowActionGroup") as? DefaultActionGroup ?: return

        val action = actionManager.getAction("ShireTerminalAction")
        if (!toolsMenu.containsAction(action)) {
            toolsMenu.add(action)
        }
    }

    private fun attachDatabaseAction() {
        val actionManager = ActionManager.getInstance()
        val toolsMenu = actionManager.getAction("DatabaseViewPopupMenu") as? DefaultActionGroup ?: return

        val action = actionManager.getAction("ShireDatabaseAction")
        if (!toolsMenu.containsAction(action)) {
            toolsMenu.add(action, Constraints.LAST)
        }
    }

    private fun attachVcsLogAction() {
        val actionManager = ActionManager.getInstance()
        val toolsMenu = actionManager.getAction("Vcs.Log.ContextMenu") as? DefaultActionGroup ?: return

        val action = actionManager.getAction("ShireVcsLogAction")
        if (!toolsMenu.containsAction(action)) {
            toolsMenu.add(action, Constraints.FIRST)
        }
    }

    private fun attachExtensionActions(project: Project) {
        project.messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC, ShireSonarLintToolWindowListener());
    }

    companion object {
        private fun obtainShireFiles(project: Project): List<DevInFile> {
            ApplicationManager.getApplication().assertReadAccessAllowed()
            val projectShire = obtainProjectShires(project).map {
                PsiManager.getInstance(project).findFile(it) as DevInFile
            }

            return projectShire
        }

        private fun obtainProjectShires(project: Project): List<VirtualFile> {
            val scope = ProjectScope.getContentScope(project)
            val projectShire = FileTypeIndex.getFiles(DevInFileType.Companion.INSTANCE, scope).mapNotNull {
                it
            }

            return projectShire
        }

        fun findShireFile(project: Project, filename: String): DevInFile? {
            return DynamicShireActionService.getInstance(project).getAllActions().map {
                it.devinFile
            }.firstOrNull {
                it.name == filename
            }
        }
    }
}