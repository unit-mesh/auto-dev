package cc.unitmesh.devti.sketch.ui

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent

interface LangSketch : Disposable {
    fun getViewText(): String
    fun updateViewText(text: String, complete: Boolean)
    fun updateLanguage(language: Language?, originLanguage: String?) {}

    fun getComponent(): JComponent
    fun addOrUpdateRenderView(component: JComponent) {}
    fun hasRenderView(): Boolean = false
    /**
     * !important, the done update text will return all text in the editor
     */
    fun onDoneStream(allText: String) {}
    fun onComplete(code: String) {}

    fun setupActionBar(project: Project, editor: Editor, isPackageFile: Boolean, showBottomBorder: Boolean = true): ActionToolbar? {
        val toolbar = collectActionBar(isPackageFile) ?: return null

        if (editor is EditorEx) {
            toolbar.component.setBackground(editor.backgroundColor)
        }

        toolbar.component.setOpaque(true)
        toolbar.targetComponent = editor.contentComponent
        editor.headerComponent = toolbar.component

        if (showBottomBorder) {
            toolbar.component.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0)
        }

        val connect = project.messageBus.connect(this)
        val topic: Topic<EditorColorsListener> = EditorColorsManager.TOPIC
        connect.subscribe(topic, EditorColorsListener {
            if (editor is EditorEx) {
                toolbar.component.setBackground(editor.backgroundColor)
            }
        })

        return toolbar
    }

    fun collectActionBar(isDeclarePackageFile: Boolean): ActionToolbar? {
        val toolbarActionGroup = if (isDeclarePackageFile) {
            ActionManager.getInstance().getAction("AutoDev.ToolWindow.Snippet.DependenciesToolbar") as? ActionGroup
                ?: return null
        } else {
            ActionManager.getInstance().getAction("AutoDev.ToolWindow.Snippet.Toolbar") as? ActionGroup
                ?: return null
        }

        val toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.MAIN_TOOLBAR, toolbarActionGroup, true)
        return toolbar
    }
}
