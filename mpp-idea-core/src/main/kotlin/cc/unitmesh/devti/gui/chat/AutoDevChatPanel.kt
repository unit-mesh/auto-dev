package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import com.intellij.openapi.project.Project
import javax.swing.JProgressBar

interface AutoDevChatPanel {
    val progressBar: JProgressBar get() = JProgressBar()
    fun resetChatSession()

    /**
     * Custom Agent Event
     */
    fun resetAgent()
    fun hasSelectedCustomAgent(): Boolean
    fun getSelectedCustomAgent(): CustomAgentConfig
    fun selectAgent(config: CustomAgentConfig)

    /**
     * Progress Bar
     */
    fun hiddenProgressBar()
    fun showProgressBar()

    /**
     * append custom view
     */
    fun appendWebView(content: String, project: Project)
}