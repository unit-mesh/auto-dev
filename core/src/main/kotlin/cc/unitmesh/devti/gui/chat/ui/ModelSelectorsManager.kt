package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.agent.custom.model.CustomAgentState
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.customize.customizeSetting
import cc.unitmesh.devti.settings.model.LLMModelManager
import cc.unitmesh.devti.settings.ui.ModelItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.JPanel

/**
 * Manages model selector and agent selector components
 */
class ModelSelectorsManager(
    private val project: Project,
    private val showAgent: Boolean = true
) {
    // Model selector
    lateinit var modelSelector: ComboBox<ModelItem>
        private set
    
    // Agent selector
    private val defaultRag: CustomAgentConfig = CustomAgentConfig("<Select Custom Agent>", "Normal")
    lateinit var customAgentBox: ComboBox<CustomAgentConfig>
    
    fun initialize(): JPanel? {
        createModelSelector()
        
        return if (project.customizeSetting.enableCustomAgent && showAgent) {
            createAgentSelector()
            createLeftPanel()
        } else {
            null
        }
    }
    
    private fun createModelSelector() {
        val modelItems = LLMModelManager.getInstance().getAllAvailableModels()
        modelSelector = ComboBox(modelItems.toTypedArray())
        modelSelector.renderer = SimpleListCellRenderer.create { label: JBLabel, value: ModelItem?, _: Int ->
            if (value != null) {
                label.text = value.displayName
            }
        }
        
        // Set current model selection
        val currentModel = AutoDevSettingsState.getInstance().defaultModelId.ifEmpty { "Default" }
        for (i in 0 until modelSelector.itemCount) {
            val item = modelSelector.getItemAt(i)
            if (item.modelId == currentModel) {
                modelSelector.selectedIndex = i
                break
            }
        }
        
        // Add action listener for model changes
        modelSelector.addActionListener {
            val selected = modelSelector.selectedItem as? ModelItem ?: return@addActionListener
            val newProvider = LLMModelManager.getInstance().getProviderFromModelId(selected.modelId)
            AutoDevSettingsState.getInstance().defaultModelId = newProvider
        }
        
        modelSelector.preferredSize = Dimension(200, modelSelector.preferredSize.height)
    }
    
    private fun createAgentSelector() {
        customAgentBox = ComboBox(MutableCollectionComboBoxModel(loadAgents()))
        customAgentBox.renderer = SimpleListCellRenderer.create { label: JBLabel, value: CustomAgentConfig?, _: Int ->
            if (value != null) {
                label.text = value.name
            }
        }
        customAgentBox.selectedItem = defaultRag
        customAgentBox.addActionListener {
            if (customAgentBox.isPopupVisible) {
                refreshAgentList()
            }
        }
    }
    
    private fun createLeftPanel(): JPanel {
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        leftPanel.add(customAgentBox)
        leftPanel.add(Box.createHorizontalStrut(JBUI.scale(8)))
        leftPanel.add(modelSelector)
        return leftPanel
    }
    
    private fun loadAgents(): MutableList<CustomAgentConfig> {
        val rags = CustomAgentConfig.loadFromProject(project)
        
        if (rags.isEmpty()) return mutableListOf(defaultRag)
        
        return (listOf(defaultRag) + rags).toMutableList()
    }
    
    private fun refreshAgentList() {
        val currentSelection = customAgentBox.selectedItem
        val agents = loadAgents()
        val model = customAgentBox.model as MutableCollectionComboBoxModel<CustomAgentConfig>
        model.update(agents)
        
        // Try to restore the previous selection
        if (currentSelection != null && agents.contains(currentSelection)) {
            customAgentBox.selectedItem = currentSelection
        } else {
            customAgentBox.selectedItem = defaultRag
        }
    }
    
    // Public API methods for agent management
    fun hasSelectedAgent(): Boolean {
        if (!project.customizeSetting.enableCustomAgent) return false
        if (!::customAgentBox.isInitialized) return false
        if (customAgentBox.selectedItem == null) return false
        return customAgentBox.selectedItem != defaultRag
    }
    
    fun getSelectedAgent(): CustomAgentConfig {
        return customAgentBox.selectedItem as CustomAgentConfig
    }
    
    fun selectAgent(config: CustomAgentConfig) {
        if (::customAgentBox.isInitialized) {
            customAgentBox.selectedItem = config
        }
    }
    
    fun resetAgent() {
        if (::customAgentBox.isInitialized) {
            (customAgentBox.selectedItem as? CustomAgentConfig)?.let {
                it.state = CustomAgentState.START
            }
            customAgentBox.selectedItem = defaultRag
        }
    }
    
    fun getCustomAgent(): ComboBox<CustomAgentConfig>? = 
        if (::customAgentBox.isInitialized) customAgentBox else null
}
