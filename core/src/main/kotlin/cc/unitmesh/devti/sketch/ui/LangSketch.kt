package cc.unitmesh.devti.sketch.ui

import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import javax.swing.JComponent

interface LangSketch : Disposable {
    fun getViewText(): String
    fun updateViewText(text: String, complete: Boolean)
    fun getComponent(): JComponent
    fun updateLanguage(language: Language?, originLanguage: String?)

    /**
     * !important, the done update text will return all text in the editor
     */
    fun onDoneStream(allText: String) {}

    fun setupActionBar(project: Project, editor: Editor, isDeclarePackageFile: Boolean) {
        val toolbar = collectActionBar(isDeclarePackageFile) ?: return

        if (editor is EditorEx) {
            toolbar.component.setBackground(editor.backgroundColor)
        }

        toolbar.component.setOpaque(true)
        toolbar.targetComponent = editor.contentComponent
        editor.headerComponent = toolbar.component

        val connect = project.messageBus.connect(this)
        val topic: Topic<EditorColorsListener> = EditorColorsManager.TOPIC
        connect.subscribe(topic, EditorColorsListener {
            if (editor is EditorEx) {
                toolbar.component.setBackground(editor.backgroundColor)
            }
        })
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
