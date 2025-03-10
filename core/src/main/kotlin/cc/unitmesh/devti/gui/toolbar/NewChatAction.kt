package cc.unitmesh.devti.gui.toolbar

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.ChatCodingPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Key
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import javax.swing.JButton
import javax.swing.JComponent

class NewChatAction : DumbAwareAction(), CustomComponentAction {
    private val logger = logger<NewChatAction>()
    val AVOID_EXTENDING_BORDER_GRAPHICS = Key.create<Boolean>("JButton.avoidExtendingBorderGraphics")

    override fun actionPerformed(e: AnActionEvent) = Unit

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button: JButton = object : JButton(AllIcons.General.Add) {
            init {
                putClientProperty("ActionToolbar.smallVariant", true)
                putClientProperty(AVOID_EXTENDING_BORDER_GRAPHICS, true)
                setHorizontalTextPosition(LEFT)
                setContentAreaFilled(false)
                setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED)
                isBorderPainted = false
                iconTextGap = 0
                preferredSize = JBDimension(32, 32)

                addActionListener {
                    val dataContext: DataContext = ActionToolbar.getDataContextFor(this)
                    val project = dataContext.getData(CommonDataKeys.PROJECT)
                    if (project == null) {
                        logger.error("project is null")
                        return@addActionListener
                    }

                    val toolWindowManager = AutoDevToolWindowFactory.getToolWindow(project) ?: return@addActionListener
                    val contentManager = toolWindowManager.contentManager

                    val codingPanel =
                        contentManager?.component?.components?.filterIsInstance<ChatCodingPanel>()?.firstOrNull()

                    AutoDevToolWindowFactory().createToolWindowContent(project, toolWindowManager)
                    if (codingPanel == null) {
                        return@addActionListener
                    }

                    codingPanel.resetChatSession()
                }
            }
        }

        return Wrapper(button).also {
            it.setBorder(JBUI.Borders.empty(0, 10))
        }
    }
}
