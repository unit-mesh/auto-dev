package cc.unitmesh.devti.mcp.editor

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class McpFileEditorWithPreview(
    private val ourEditor: TextEditor,
    private var preview: McpPreviewEditor,
    private val project: Project,
) : TextEditorWithPreview(ourEditor, preview, "MCP Split Editor", Layout.SHOW_EDITOR_AND_PREVIEW) {
    val virtualFile: VirtualFile = ourEditor.file

    init {
        preview.setMainEditor(ourEditor.editor)
    }

    override fun dispose() {
        TextEditorProvider.getInstance().disposeEditor(ourEditor)
    }

    override fun createToolbar(): ActionToolbar {
        return ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, createActionGroup(project), true)
            .also {
                it.targetComponent = editor.contentComponent
            }
    }

    private fun createActionGroup(project: Project): ActionGroup {
        return DefaultActionGroup(
            object : AnAction(
                AutoDevBundle.message("mcp.preview.editor.title"),
                AutoDevBundle.message("mcp.preview.editor.title"),
                AllIcons.Actions.Preview
            ) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = !DumbService.isDumb(project)
                }

                override fun actionPerformed(e: AnActionEvent) {
                    DumbService.getInstance(project).runWhenSmart {
                        preview.component.isVisible = true
                        preview.refreshMcpTool()
                    }
                }
            },
            object : AnAction(
                AutoDevBundle.message("mcp.editor.refresh.title"),
                AutoDevBundle.message("mcp.editor.refresh.title"),
                AllIcons.Actions.Refresh
            ) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = !DumbService.isDumb(project)
                }

                override fun actionPerformed(e: AnActionEvent) {
                    DumbService.getInstance(project).runWhenSmart {
                        preview.refreshMcpTool()
                    }
                }
            }
        )
    }
}