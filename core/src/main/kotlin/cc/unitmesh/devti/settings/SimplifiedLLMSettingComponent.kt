package cc.unitmesh.devti.settings

import cc.unitmesh.devti.llm2.GithubCopilotManager
import cc.unitmesh.devti.settings.dialog.QuickLLMSetupDialog
import cc.unitmesh.devti.settings.locale.HUMAN_LANGUAGES
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.i18nLabel
import cc.unitmesh.devti.settings.model.LLMModelManager
import cc.unitmesh.devti.settings.ui.DeleteButtonEditor
import cc.unitmesh.devti.settings.ui.DeleteButtonRenderer
import cc.unitmesh.devti.settings.ui.ModelItem
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * Simplified LLM Setting Component that uses extracted components
 */
class SimplifiedLLMSettingComponent(private val settings: AutoDevSettingsState) {

    // Basic settings
    private val languageParam by LLMParam.creating({ LanguageChangedCallback.language = it }) {
        ComboBox(settings.language, HUMAN_LANGUAGES.entries.map { it.display })
    }
    private val delaySecondsParam by LLMParam.creating { Editable(settings.delaySeconds) }
    private val maxTokenLengthParam by LLMParam.creating { Editable(settings.maxTokenLength) }

    // Default model configuration
    private val defaultModelDropdown = ComboBox<ModelItem>()
    private val useDefaultForAllCheckbox = JBCheckBox("Use default model for all categories", settings.useDefaultForAllCategories)

    // Category-specific model dropdowns (only shown when not using default for all)
    private val planLLMDropdown = ComboBox<ModelItem>()
    private val actLLMDropdown = ComboBox<ModelItem>()
    private val completionLLMDropdown = ComboBox<ModelItem>()
    private val embeddingLLMDropdown = ComboBox<ModelItem>()
    private val fastApplyLLMDropdown = ComboBox<ModelItem>()

    // Category section panel for dynamic visibility
    private var categoryPanel: JPanel? = null

    // Model management - table with Name, Model, Streaming, Temperature, Delete
    private val llmTableModel = object : DefaultTableModel(arrayOf("Name", "Model", "Streaming", "Temperature", "Delete"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return column == 4 // Only delete column is "editable" (clickable)
        }
    }
    private val llmTable = JTable(llmTableModel)

    // Track if settings have been modified
    private var isModified = false
    private var isInitializing = false

    private val project = ProjectManager.getInstance().openProjects.firstOrNull()

    // Model manager for handling LLM models
    private val modelManager = LLMModelManager(
        project = project,
        settings = settings,
        onModified = { markAsModified() }
    )

    private val formBuilder: FormBuilder = FormBuilder.createFormBuilder()
    val panel: JPanel get() = formBuilder.panel

    init {
        setupEventListeners()
        setupTableEventListeners()

        // Try to initialize GitHub Copilot early
        initializeGitHubCopilot()

        applySettings(settings)
        LanguageChangedCallback.language = AutoDevSettingsState.getInstance().language
    }

    private fun initializeGitHubCopilot() {
        val manager = service<GithubCopilotManager>()
        if (!manager.isInitialized()) {
            // Initialize in background and update UI when ready
            modelManager.refreshGitHubCopilotModels(
                defaultModelDropdown,
                planLLMDropdown,
                actLLMDropdown,
                completionLLMDropdown,
                embeddingLLMDropdown,
                fastApplyLLMDropdown,
                llmTableModel
            )
        }
    }

    private fun setupEventListeners() {
        // When checkbox changes, show/hide category dropdowns
        useDefaultForAllCheckbox.addActionListener {
            updateCategoryDropdownsVisibility()
            markAsModified()
        }

        // Add change listeners to all dropdowns
        defaultModelDropdown.addActionListener { markAsModified() }
        planLLMDropdown.addActionListener { markAsModified() }
        actLLMDropdown.addActionListener { markAsModified() }
        completionLLMDropdown.addActionListener { markAsModified() }
        embeddingLLMDropdown.addActionListener { markAsModified() }
        fastApplyLLMDropdown.addActionListener { markAsModified() }
    }

    private fun setupTableEventListeners() {
        // Set up delete button renderer and editor
        llmTable.getColumn("Delete").cellRenderer = DeleteButtonRenderer(modelManager::isCustomLLM)
        llmTable.getColumn("Delete").cellEditor = DeleteButtonEditor(
            isCustomLLM = modelManager::isCustomLLM,
            onDelete = { row -> modelManager.deleteLLMAtRow(llmTable, row) }
        )

        // Set up double-click listener for editing
        llmTable.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val row = llmTable.rowAtPoint(e.point)
                    val column = llmTable.columnAtPoint(e.point)

                    // Only allow double-click on Name, Model, Streaming, Temperature columns (not Delete column)
                    if (row >= 0 && column < 4) {
                        modelManager.editLLMAtRow(llmTable, row)
                    }
                }
            }
        })

        // Set column widths
        llmTable.columnModel.getColumn(0).preferredWidth = 150 // Name
        llmTable.columnModel.getColumn(1).preferredWidth = 200 // Model
        llmTable.columnModel.getColumn(2).preferredWidth = 80  // Streaming
        llmTable.columnModel.getColumn(3).preferredWidth = 100 // Temperature
        llmTable.columnModel.getColumn(4).preferredWidth = 80  // Delete
        llmTable.columnModel.getColumn(4).maxWidth = 80
    }

    private fun markAsModified() {
        if (!isInitializing) {
            isModified = true
        }
    }

    private fun updateCategoryDropdownsVisibility() {
        // Toggle visibility of category panel without rebuilding entire UI
        categoryPanel?.isVisible = !useDefaultForAllCheckbox.isSelected

        // Trigger layout update
        panel.revalidate()
        panel.repaint()
    }

    private fun createCategoryPanel(): JPanel {
        val categoryFormBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Category-Specific Models"), JPanel(), 1, false)
            .addLabeledComponent(JBLabel("Plan:"), planLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Act:"), actLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Completion:"), completionLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("Embedding:"), embeddingLLMDropdown, 1, false)
            .addLabeledComponent(JBLabel("FastApply:"), fastApplyLLMDropdown, 1, false)
            .addSeparator()

        return categoryFormBuilder.panel
    }

    // Build the main settings panel
    fun applySettings(settings: AutoDevSettingsState, updateParams: Boolean = false) {
        isInitializing = true
        panel.removeAll()

        // Update dropdowns and table
        modelManager.updateAllDropdowns(
            defaultModelDropdown,
            planLLMDropdown,
            actLLMDropdown,
            completionLLMDropdown,
            embeddingLLMDropdown,
            fastApplyLLMDropdown
        )
        modelManager.updateLLMTable(llmTableModel)
        modelManager.setSelectedModels(
            settings,
            defaultModelDropdown,
            planLLMDropdown,
            actLLMDropdown,
            completionLLMDropdown,
            embeddingLLMDropdown,
            fastApplyLLMDropdown
        )

        // Create add LLM button
        val addLLMButton = JButton("Add Customs LLM")
        addLLMButton.addActionListener { modelManager.createNewLLM() }

        // Create quick setup button
        val quickSetupButton = JButton("Quick LLM Setup")
        quickSetupButton.addActionListener { showQuickSetupDialog() }

        // Create refresh button for GitHub Copilot models
        val refreshButton = JButton("Refresh GitHub Copilot Models")
        refreshButton.addActionListener {
            modelManager.refreshGitHubCopilotModels(
                defaultModelDropdown,
                planLLMDropdown,
                actLLMDropdown,
                completionLLMDropdown,
                embeddingLLMDropdown,
                fastApplyLLMDropdown,
                llmTableModel
            )
        }

        // Create button panel
        val buttonPanel = JPanel()
        buttonPanel.add(quickSetupButton)
        buttonPanel.add(addLLMButton)
        buttonPanel.add(refreshButton)

        // Create category panel separately for dynamic visibility
        categoryPanel = createCategoryPanel()

        formBuilder
            // Basic Settings Section
            .addLabeledComponent(JBLabel("Basic Settings"), JPanel(), 1, false)
            .addLLMParam(languageParam)
            .addLLMParam(maxTokenLengthParam)
            .addLLMParam(delaySecondsParam)
            .addSeparator()

            // Default Model Configuration Section
            .addLabeledComponent(JBLabel("Default Model Configuration"), JPanel(), 1, false)
            .addLabeledComponent(JBLabel("Default Model:"), defaultModelDropdown, 1, false)
            .addComponent(useDefaultForAllCheckbox)
            .addSeparator()

        // Add category panel (visibility controlled dynamically)
        formBuilder.addComponent(categoryPanel!!)

        // Create a properly sized scroll pane for the table
        val tableScrollPane = JScrollPane(llmTable).apply {
            preferredSize = Dimension(600, 200)
            minimumSize = Dimension(400, 150)
        }

        formBuilder
            // Model Management Section
            .addLabeledComponent(JBLabel("Model Management"), JPanel(), 1, false)
            .addComponent(buttonPanel)
            .addComponentFillVertically(tableScrollPane, 0)
            .addComponentFillVertically(JPanel(), 0)

        // Set initial visibility
        updateCategoryDropdownsVisibility()

        panel.invalidate()
        panel.repaint()

        isInitializing = false
        isModified = false  // Reset modified flag after applying settings
    }

    // Export settings to AutoDevSettingsState
    fun exportSettings(destination: AutoDevSettingsState) {
        destination.apply {
            maxTokenLength = maxTokenLengthParam.value
            language = languageParam.value
            delaySeconds = delaySecondsParam.value

            // Export default model configuration
            defaultModelId = (defaultModelDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            useDefaultForAllCategories = useDefaultForAllCheckbox.isSelected

            // Export category-specific model selections
            selectedPlanModelId = (planLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedActModelId = (actLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedCompletionModelId = (completionLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedEmbeddingModelId = (embeddingLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""
            selectedFastApplyModelId = (fastApplyLLMDropdown.selectedItem as? ModelItem)?.modelId ?: ""

            // Custom LLMs are already saved in the customLlms field when created/edited
        }
    }

    // Check if settings have been modified
    fun isModified(settings: AutoDevSettingsState): Boolean {
        return isModified ||
                settings.maxTokenLength != maxTokenLengthParam.value ||
                settings.language != languageParam.value ||
                settings.delaySeconds != delaySecondsParam.value ||
                settings.defaultModelId != ((defaultModelDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.useDefaultForAllCategories != useDefaultForAllCheckbox.isSelected ||
                settings.selectedPlanModelId != ((planLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedActModelId != ((actLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedCompletionModelId != ((completionLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedEmbeddingModelId != ((embeddingLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "") ||
                settings.selectedFastApplyModelId != ((fastApplyLLMDropdown.selectedItem as? ModelItem)?.modelId ?: "")
    }

    // Helper extension function for FormBuilder
    private fun FormBuilder.addLLMParam(llmParam: LLMParam): FormBuilder = apply {
        when (llmParam.type) {
            LLMParam.ParamType.Text -> {
                addLabeledComponent(i18nLabel(llmParam.label), ReactiveTextField(llmParam) {
                    isEnabled = it.isEditable
                }, 1, false)
            }
            LLMParam.ParamType.ComboBox -> {
                addLabeledComponent(i18nLabel(llmParam.label), ReactiveComboBox(llmParam), 1, false)
            }
            else -> {
                addSeparator()
            }
        }
    }

    /**
     * Show quick setup dialog with predefined LLM configurations
     */
    private fun showQuickSetupDialog() {
        val quickSetupDialog = QuickLLMSetupDialog(project, settings) {
            // Refresh the UI after adding new LLM
            modelManager.updateAllDropdowns(
                defaultModelDropdown,
                planLLMDropdown,
                actLLMDropdown,
                completionLLMDropdown,
                embeddingLLMDropdown,
                fastApplyLLMDropdown
            )
            modelManager.updateLLMTable(llmTableModel)
            markAsModified()
        }
        quickSetupDialog.show()
    }
}
