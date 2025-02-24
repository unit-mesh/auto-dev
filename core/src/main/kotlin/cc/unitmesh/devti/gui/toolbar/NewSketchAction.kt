package cc.unitmesh.devti.gui.toolbar

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.sketch.SketchToolWindow
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import javax.swing.JButton
import javax.swing.JComponent

class NewSketchAction : AnAction("New Sketch", "Create new Sketch", AllIcons.General.Add), CustomComponentAction {
    private val logger = logger<NewChatAction>()

    override fun update(e: AnActionEvent) {
        e.presentation.text = "New Sketch"
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    override fun actionPerformed(e: AnActionEvent) {
        newSketch(e.dataContext)
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button: JButton = object : JButton() {
            init {
                putClientProperty("ActionToolbar.smallVariant", true)
                putClientProperty("customButtonInsets", JBInsets(1, 1, 1, 1).asUIResource())

                setOpaque(false)
                addActionListener {
                    val dataContext: DataContext = ActionToolbar.getDataContextFor(this)
                    newSketch(dataContext)
                }
            }
        }.apply {
            componentStateChanged("chat.panel.newSketch", this) { b, d -> b.text = d }
        }

        return Wrapper(button).also {
            it.setBorder(JBUI.Borders.empty(0, 10))
        }
    }

    private fun newSketch(dataContext: DataContext) {
        val project = dataContext.getData(CommonDataKeys.PROJECT)
        if (project == null) {
            logger.error("project is null")
            return
        }

        val toolWindowManager = AutoDevToolWindowFactory.getToolWindow(project)
        val contentManager = toolWindowManager?.contentManager

        val sketchPanel =
            contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()?.firstOrNull()

        if (sketchPanel == null) {
            AutoDevToolWindowFactory.createSketchToolWindow(project, toolWindowManager!!)
        }

        sketchPanel?.resetSketchSession()

        // focus on sketch panel
        contentManager?.contents?.filter { it.component is SketchToolWindow }?.forEach {
            contentManager.setSelectedContent(it)
        }
    }
}
