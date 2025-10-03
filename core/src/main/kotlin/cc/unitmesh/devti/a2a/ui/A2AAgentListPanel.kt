package cc.unitmesh.devti.a2a.ui

import cc.unitmesh.devti.a2a.A2AClientConsumer
import cc.unitmesh.devti.a2a.A2aServer
import cc.unitmesh.devti.mcp.client.McpServer
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import io.a2a.spec.AgentCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class A2AAgentListPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val a2aClientConsumer = A2AClientConsumer()
    private val textGray = JBColor(0x6B7280, 0x9DA0A8)

    private fun getAgentName(agent: AgentCard): String = agent.name()
    private fun getAgentDescription(agent: AgentCard): String? = agent.description()
    private fun getProviderName(agent: AgentCard): String? = agent.provider()?.organization()

    private var loadingJob: Job? = null
    private val serverLoadingStatus = mutableMapOf<String, Boolean>()
    private val serverPanels = mutableMapOf<String, JPanel>()
    private val allA2AAgents = mutableMapOf<String, List<AgentCard>>()
    private val currentFilteredAgents = mutableMapOf<String, List<AgentCard>>()

    init {
        background = UIUtil.getPanelBackground()
        border = JBUI.Borders.empty()
        layout = BorderLayout()
    }

    fun loadAgents(content: String, onAgentsLoaded: (MutableMap<String, List<AgentCard>>) -> Unit = {}) {
        val serversToUse = getA2AServersFromConfig(content)
        loadingJob?.cancel()
        serverLoadingStatus.clear()
        serverPanels.clear()
        allA2AAgents.clear()
        currentFilteredAgents.clear()

        SwingUtilities.invokeLater {
            removeAll()
            revalidate()
            repaint()
        }

        if (serversToUse.isNullOrEmpty()) {
            SwingUtilities.invokeLater {
                showNoServersMessage()
            }
            return
        }

        loadingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize A2A clients with all servers
                val serverList = serversToUse.values.toList()
                a2aClientConsumer.init(serverList)

                SwingUtilities.invokeLater {
                    serversToUse.forEach { (serverName, server) ->
                        serverLoadingStatus[serverName] = true
                        createServerSection(serverName)
                    }
                }

                // Load agents from all servers
                val jobs = serversToUse.map { (serverName, server) ->
                    launch {
                        try {
                            val agents = a2aClientConsumer.listAgents()

                            synchronized(allA2AAgents) {
                                allA2AAgents[serverName] = agents
                                currentFilteredAgents[serverName] = agents
                            }

                            SwingUtilities.invokeLater {
                                updateServerSection(serverName, agents)
                                serverLoadingStatus[serverName] = false
                            }
                        } catch (e: Exception) {
                            SwingUtilities.invokeLater {
                                showServerError(serverName, e.message ?: "Unknown error")
                                serverLoadingStatus[serverName] = false
                            }
                        }
                    }
                }

                jobs.joinAll()
                onAgentsLoaded(allA2AAgents)
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    showGeneralError(e.message ?: "Failed to load A2A agents")
                }
            }
        }
    }

    fun filterAgents(searchText: String) {
        if (searchText.isEmpty()) {
            currentFilteredAgents.clear()
            currentFilteredAgents.putAll(allA2AAgents)
        } else {
            currentFilteredAgents.clear()
            allA2AAgents.forEach { (serverUrl, agents) ->
                val filtered = agents.filter { agent ->
                    getAgentName(agent).contains(searchText, ignoreCase = true) ||
                            getAgentDescription(agent)?.contains(searchText, ignoreCase = true) == true ||
                            getProviderName(agent)?.contains(searchText, ignoreCase = true) == true
                }
                if (filtered.isNotEmpty()) {
                    currentFilteredAgents[serverUrl] = filtered
                }
            }
        }

        SwingUtilities.invokeLater {
            updateAllServerSections()
        }
    }

    private fun createServerSection(serverUrl: String) {
        val serverPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(8, 0)
        }

        val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = UIUtil.getPanelBackground()
        }

        val serverLabel = JBLabel("A2A Server: $serverUrl").apply {
            font = JBUI.Fonts.label(13.0f).asBold()
        }

        val loadingLabel = JBLabel("Loading...").apply {
            font = JBUI.Fonts.label(12.0f)
            foreground = textGray
        }

        headerPanel.add(serverLabel)
        headerPanel.add(loadingLabel)

        val agentsPanel = JPanel(GridBagLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(4, 0)
        }

        serverPanel.add(headerPanel, BorderLayout.NORTH)
        serverPanel.add(agentsPanel, BorderLayout.CENTER)

        serverPanels[serverUrl] = agentsPanel

        add(serverPanel)
        revalidate()
        repaint()
    }

    private fun updateServerSection(serverUrl: String, agents: List<AgentCard>) {
        val agentsPanel = serverPanels[serverUrl] ?: return
        agentsPanel.removeAll()

        if (agents.isEmpty()) {
            val noAgentsLabel = JBLabel("No agents available for $serverUrl").apply {
                foreground = textGray
                horizontalAlignment = SwingConstants.LEFT
            }
            agentsPanel.add(noAgentsLabel)
        } else {
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.NORTHWEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                insets = JBUI.insets(2)
            }

            agents.forEach { agent ->
                val panel = A2AAgentCardPanel(project, agent, a2aClientConsumer)
                agentsPanel.add(panel, gbc)
                gbc.gridy++
            }

            // Add a filler component to push cards to the top
            gbc.weighty = 1.0
            gbc.fill = GridBagConstraints.BOTH
            agentsPanel.add(JPanel().apply { background = UIUtil.getPanelBackground() }, gbc)
        }

        agentsPanel.revalidate()
        agentsPanel.repaint()
    }

    private fun updateAllServerSections() {
        currentFilteredAgents.forEach { (serverUrl, agents) ->
            updateServerSection(serverUrl, agents)
        }
    }

    private fun showServerError(serverUrl: String, errorMessage: String) {
        val agentsPanel = serverPanels[serverUrl] ?: return
        agentsPanel.removeAll()

        val errorLabel = JBLabel("Error loading agents from $serverUrl: $errorMessage").apply {
            foreground = JBColor.RED
            horizontalAlignment = SwingConstants.LEFT
        }
        agentsPanel.add(errorLabel)
        agentsPanel.revalidate()
        agentsPanel.repaint()
    }

    private fun showNoServersMessage() {
        removeAll()

        val noServersPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(16)
        }

        val noServersLabel = JBLabel("No A2A servers configured. Please check your configuration.").apply {
            foreground = textGray
            horizontalAlignment = SwingConstants.CENTER
        }

        noServersPanel.add(noServersLabel, BorderLayout.CENTER)
        add(noServersPanel)
        revalidate()
        repaint()
    }

    private fun showGeneralError(errorMessage: String) {
        removeAll()

        val errorPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(16)
        }

        val errorLabel = JBLabel("Error: $errorMessage").apply {
            foreground = JBColor.RED
            horizontalAlignment = SwingConstants.CENTER
        }

        errorPanel.add(errorLabel, BorderLayout.CENTER)
        add(errorPanel)
        revalidate()
        repaint()
    }

    fun dispose() {
        loadingJob?.cancel()
    }

    fun getAgents(): Map<String, List<AgentCard>> = allA2AAgents

    fun getA2AClientConsumer(): A2AClientConsumer = a2aClientConsumer

    private fun getA2AServersFromConfig(content: String): Map<String, A2aServer>? {
        val mcpConfig = McpServer.load(content)
        return mcpConfig?.a2aServers?.filter { entry ->
            // A2A servers don't have a disabled flag like MCP servers, so we include all
            true
        }
    }
}
