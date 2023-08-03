package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.InternalDecorator
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min

class AutoDevInputWithButton(
    private val chatCodingService: ChatCodingService,
    private val project: Project,
    disposable: Disposable
) : BorderLayoutPanel() {
    private val input: AutoDevInputField
    private val button: ActionButton

    init {
        val presentation = Presentation(AutoDevBundle.message("devti.chat.send"))
        val jButton = JButton(AutoDevBundle.message("devti.chat.send"))
        presentation.icon = jButton.icon

        input = AutoDevInputField(project, listOf())
        button = ActionButton(
            DumbAwareAction.create {
                object : AnAction() {
                    override fun actionPerformed(e1: AnActionEvent) {

                    }
                }
            },
            presentation,
            "",
            Dimension(20, 20)
        )
    }

    override fun getPreferredSize(): Dimension {
        val result = super.getPreferredSize()
        result.height = max(min(result.height, maxHeight), minimumSize.height)
        return result
    }

    private val maxHeight: Int get() {
            val decorator: InternalDecorator = UIUtil.getParentOfType(
                InternalDecorator::class.java, this as Component
            )!!

        val contentManager: ContentManager? = decorator.getContentManager()
        if (contentManager != null) {
            val component = contentManager.component
            val toolWindowHeight = component.height
            return toolWindowHeight / 2
        }

        return JBUI.scale(200)
        }
    val focusableComponent: JComponent
        get() = input
}