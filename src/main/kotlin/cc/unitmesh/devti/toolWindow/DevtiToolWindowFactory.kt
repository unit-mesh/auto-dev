package cc.unitmesh.devti.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import cc.unitmesh.devti.DevtiBundle
import cc.unitmesh.devti.services.MyProjectService
import com.intellij.ui.components.JBTextField
import javax.swing.JButton

class DevtiToolWindowFactory : ToolWindowFactory {
    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    private val contentFactory = ContentFactory.SERVICE.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val devtiToolWindow = DevtiToolWindow(toolWindow)
        val content = contentFactory.createContent(devtiToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class DevtiToolWindow(toolWindow: ToolWindow) {
        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(DevtiBundle.message("configGithubToken", "?"))
            add(label)

            // create input field for GitHub token
            val githubTokenInput = JBTextField().apply {
                columns = 30
                text = service.githubToken
            }
            add(githubTokenInput)

            // add button and event to save GitHub token
            val saveGithubTokenButton = JButton("Save")
            saveGithubTokenButton.addActionListener {
                service.githubToken = githubTokenInput.text
            }

            val label2 = JBLabel(DevtiBundle.message("configOpenAIToken", "?"))
            add(label2)
            // configure for OpenAI Token
            val OpenAITokenInput = JBTextField().apply {
                columns = 30
                text = service.OpenAIToken
            }
            add(OpenAITokenInput)

            val saveOpenAITokenButton = JButton("Save")
            saveOpenAITokenButton.addActionListener {
                service.OpenAIToken = OpenAITokenInput.text
            }
        }
    }
}
