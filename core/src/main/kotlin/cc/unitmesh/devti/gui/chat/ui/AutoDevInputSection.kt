package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.agent.custom.model.CustomAgentState
import cc.unitmesh.devti.completion.AutoDevInputLookupManagerListener
import cc.unitmesh.devti.gui.chat.ui.file.RelatedFileListCellRenderer
import cc.unitmesh.devti.gui.chat.ui.file.RelatedFileListViewModel
import cc.unitmesh.devti.gui.chat.ui.file.WorkspaceFilePanel
import cc.unitmesh.devti.gui.chat.ui.file.WorkspaceFileToolbar
import cc.unitmesh.devti.indexer.DomainDictService
import cc.unitmesh.devti.indexer.usage.PromptEnhancer
import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.llms.tokenizer.TokenizerFactory
import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.customize.customizeSetting
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.ide.IdeTooltip
import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.Balloon.Position
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.impl.InternalDecorator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.temporary.gui.block.AutoDevCoolBorder
import com.intellij.ui.HintHint
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.function.Supplier
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

class AutoDevInputSection(private val project: Project, val disposable: Disposable?, showAgent: Boolean = true) :
    BorderLayoutPanel() {
    private val input: AutoDevInput
    private val documentListener: DocumentListener
    private val sendButtonPresentation: Presentation
    private val stopButtonPresentation: Presentation
    private val enhanceButtonPresentation: Presentation
    private val sendButton: ActionButton
    private val stopButton: ActionButton
    private val enhanceButton: ActionButton
    private var buttonPanel: JPanel = JPanel(CardLayout())
    private val inputPanel = BorderLayoutPanel()
    val focusableComponent: JComponent get() = input

    private val relatedFileListViewModel = RelatedFileListViewModel(project)
    private val elementsList = JBList(relatedFileListViewModel.getListModel())

    private val workspaceFilePanel: WorkspaceFilePanel

    private val defaultRag: CustomAgentConfig = CustomAgentConfig("<Select Custom Agent>", "Normal")
    private var customAgent: ComboBox<CustomAgentConfig> = ComboBox(MutableCollectionComboBoxModel(listOf()))

    private val logger = logger<AutoDevInputSection>()

    val editorListeners = EventDispatcher.create(AutoDevInputListener::class.java)
    private var tokenizer: Tokenizer? = try {
        lazy { TokenizerFactory.createTokenizer() }.value
    } catch (e: Exception) {
        logger.error("TokenizerImpl.INSTANCE is not available", e)
        null
    }

    fun renderText(): String {
        val files = workspaceFilePanel.getAllFilesFormat()
        relatedFileListViewModel.clearAllFiles()
        workspaceFilePanel.clear()
        return input.text + "\n" + files
    }

    fun clearText() {
        input.recreateDocument()
        input.text = ""
    }

    fun setText(text: String) {
        input.recreateDocument()
        input.text = text
    }

    init {
        input = AutoDevInput(project, listOf(), disposable, this)
        workspaceFilePanel = WorkspaceFilePanel(project)

        setupElementsList()
        val sendButtonPresentation = Presentation(AutoDevBundle.message("chat.panel.send"))
        sendButtonPresentation.icon = AutoDevIcons.SEND
        this.sendButtonPresentation = sendButtonPresentation

        this.stopButtonPresentation = Presentation("Stop").apply {
            icon = AutoDevIcons.STOP
        }
        this.enhanceButtonPresentation = Presentation("Enhance").apply {
            icon = AutoDevIcons.MAGIC
            isEnabled = project.service<DomainDictService>().loadContent()?.isNotEmpty() == true
        }

        sendButton = ActionButton(
            DumbAwareAction.create {
                editorListeners.multicaster.onSubmit(this@AutoDevInputSection, AutoDevInputTrigger.Button)
            },
            this.sendButtonPresentation, "", Dimension(20, 20)
        )

        stopButton = ActionButton(
            DumbAwareAction.create {
                editorListeners.multicaster.onStop(this@AutoDevInputSection)
            },
            this.stopButtonPresentation, "", Dimension(20, 20)
        )

        enhanceButton = ActionButton(
            DumbAwareAction.create {
                enhancePrompt()
            },
            this.enhanceButtonPresentation, "", Dimension(20, 20)
        )

        documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val i = input.preferredSize?.height
                if (i != input.height) {
                    revalidate()
                }
            }
        }

        input.addDocumentListener(documentListener)
        input.recreateDocument()
        input.border = JBEmptyBorder(10)

        val layoutPanel = BorderLayoutPanel()
        val horizontalGlue = Box.createHorizontalGlue()
        horizontalGlue.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                IdeFocusManager.getInstance(project).requestFocus(input, true)
                input.caretModel.moveToOffset(input.text.length - 1)
            }
        })
        layoutPanel.setOpaque(false)

        if (project.customizeSetting.enableCustomAgent && showAgent) {
            customAgent = ComboBox(MutableCollectionComboBoxModel(loadRagApps()))
            customAgent.renderer = SimpleListCellRenderer.create { label: JBLabel, value: CustomAgentConfig?, _: Int ->
                if (value != null) {
                    label.text = value.name
                }
            }
            customAgent.selectedItem = defaultRag

            input.minimumSize = Dimension(input.minimumSize.width, 64)
            layoutPanel.addToLeft(customAgent)
        }

        buttonPanel = createButtonPanel()

        layoutPanel.addToCenter(horizontalGlue)
        layoutPanel.addToRight(buttonPanel)

        inputPanel.add(input, BorderLayout.CENTER)
        inputPanel.addToBottom(layoutPanel)

        inputPanel.addToTop(workspaceFilePanel)

        val scrollPane = JBScrollPane(elementsList)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        val toolbar = WorkspaceFileToolbar.createToolbar(project, relatedFileListViewModel, input)

        val headerPanel = JPanel(BorderLayout())
        headerPanel.add(toolbar, BorderLayout.NORTH)
        headerPanel.add(scrollPane, BorderLayout.CENTER)

        this.add(headerPanel, BorderLayout.NORTH)
        this.add(inputPanel, BorderLayout.CENTER)

        ComponentValidator(disposable!!).withValidator(Supplier<ValidationInfo?> {
            val validationInfo: ValidationInfo? = this.getInputValidationInfo()
            sendButton.setEnabled(validationInfo == null)
            return@Supplier validationInfo
        }).installOn((this as JComponent)).revalidate()

        addListener(object : AutoDevInputListener {
            override fun editorAdded(editor: EditorEx) {
                this@AutoDevInputSection.initEditor()
            }
        })

        setupEditorListener()
        setupRelatedListener()

        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        currentFile?.let {
            relatedFileListViewModel.addFileIfAbsent(currentFile, first = true)
        }
    }

    private fun enhancePrompt() {
        val originalIcon = enhanceButtonPresentation.icon
        enhanceButtonPresentation.icon = AutoDevIcons.InProgress
        enhanceButtonPresentation.isEnabled = false

        try {
            val content = project.service<PromptEnhancer>().create(input.text)
            val code = CodeFence.parse(content).text
            this.setText(code)
        } catch (e: Exception) {
            logger.error("Failed to enhance prompt", e)
            AutoDevNotifications.error(project, e.message ?: "An error occurred while enhancing the prompt")
        } finally {
            enhanceButtonPresentation.icon = originalIcon
            enhanceButtonPresentation.isEnabled = true
        }
    }

    private fun createButtonPanel(): JPanel {
        val panel = JPanel(CardLayout())

        // Create a panel for the "Send" state that contains both enhance and send buttons
        val sendPanel = JPanel(FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0))
        sendPanel.isOpaque = false
        sendPanel.add(enhanceButton)
        sendPanel.add(sendButton)

        panel.add(sendPanel, "Send")
        panel.add(stopButton, "Stop")

        return panel
    }

    private fun setupEditorListener() {
        project.messageBus.connect(disposable!!).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file = event.newFile ?: return
                    ApplicationManager.getApplication().invokeLater {
                        relatedFileListViewModel.addFileIfAbsent(file, true)
                    }
                }
            }
        )
    }

    private fun setupRelatedListener() {
        project.messageBus.connect(disposable!!)
            .subscribe(LookupManagerListener.TOPIC, AutoDevInputLookupManagerListener(project) {
                ApplicationManager.getApplication().invokeLater {
                    val relatedElements = RelatedClassesProvider.provide(it.language)?.lookupIO(it)
                    updateElements(relatedElements)
                }
            })
    }

    private fun setupElementsList() {
        elementsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        elementsList.layoutOrientation = JList.HORIZONTAL_WRAP
        elementsList.visibleRowCount = 2
        elementsList.cellRenderer = RelatedFileListCellRenderer(project)
        elementsList.setEmptyText("")

        elementsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                calculateRelativeFile(e)
            }
        })
    }

    private fun calculateRelativeFile(e: MouseEvent) {
        val list = e.source as JBList<*>
        val index = list.locationToIndex(e.point)
        if (index == -1) return

        val wrapper = relatedFileListViewModel.getListModel().getElementAt(index)
        val cellBounds = list.getCellBounds(index, index)

        val actionType = relatedFileListViewModel.determineFileAction(wrapper, e.point, cellBounds)
        val actionPerformed = relatedFileListViewModel.handleFileAction(wrapper, actionType) { vfile, relativePath ->
            if (relativePath != null) {
                workspaceFilePanel.addFileToWorkspace(vfile)
                ApplicationManager.getApplication().invokeLater {
                    if (!vfile.isValid) return@invokeLater
                    val psiFile = PsiManager.getInstance(project).findFile(vfile) ?: return@invokeLater
                    val relatedElements = RelatedClassesProvider.provide(psiFile.language)?.lookupIO(psiFile)
                    updateElements(relatedElements)
                }
            }
        }

        if (!actionPerformed) {
            list.clearSelection()
        }
    }

    private fun updateElements(elements: List<PsiElement>?) {
        elements?.forEach { relatedFileListViewModel.addFileIfAbsent(it.containingFile.virtualFile) }
    }

    fun showStopButton() {
        (buttonPanel.layout as? CardLayout)?.show(buttonPanel, "Stop")
        stopButton.isEnabled = true
    }

    fun showTooltip(text: String) {
        showTooltip(input, Position.above, text)
    }

    fun showTooltip(component: JComponent, position: Position, text: String) {
        val point = Point(component.x, component.y)
        val hintHint = HintHint(component, point).setAwtTooltip(true).setPreferredPosition(position)
        // Inspired by com.intellij.ide.IdeTooltipManager#showTooltipForEvent
        val tipComponent = IdeTooltipManager.initPane(text, hintHint, null)
        val tooltip = IdeTooltip(component, point, tipComponent)
        IdeTooltipManager.getInstance().show(tooltip, true)
    }

    fun send() {
        editorListeners.multicaster.onSubmit(this, AutoDevInputTrigger.Button)
    }

    fun showSendButton() {
        (buttonPanel.layout as? CardLayout)?.show(buttonPanel, "Send")
        buttonPanel.isEnabled = true
    }

    private fun loadRagApps(): List<CustomAgentConfig> {
        val rags = CustomAgentConfig.loadFromProject(project)

        if (rags.isEmpty()) return listOf(defaultRag)

        return listOf(defaultRag) + rags
    }

    fun initEditor() {
        val editorEx = this.input.editor as? EditorEx ?: return
        inputPanel.setBorder(AutoDevCoolBorder(editorEx, this))
        UIUtil.setOpaqueRecursively(this, false)
        this.revalidate()
    }

    override fun getPreferredSize(): Dimension {
        val result = com.intellij.openapi.application.runReadAction { super.getPreferredSize() }
        result.height = max(min(result.height, maxHeight), minimumSize.height)
        return result
    }

    fun selectAgent(config: CustomAgentConfig) {
        customAgent.selectedItem = config
    }

    fun setContent(trimMargin: String) {
        val focusManager = IdeFocusManager.getInstance(project)
        focusManager.requestFocus(input, true)
        this.input.recreateDocument()
        this.input.text = trimMargin
    }

    override fun getBackground(): Color? {
        // it seems that the input field is not ready when this method is called
        if (this.input == null) return super.getBackground()

        val editor = input.editor ?: return super.getBackground()
        return editor.colorsScheme.defaultBackground
    }

    override fun setBackground(bg: Color?) {}

    fun addListener(listener: AutoDevInputListener) {
        editorListeners.addListener(listener)
    }

    private fun getInputValidationInfo(): ValidationInfo? {
        val text = input.document.text
        val textLength = (this.tokenizer)?.count(text) ?: text.length

        val exceed: Int = textLength - AutoDevSettingsState.maxTokenLength
        if (exceed <= 0) return null

        val errorMessage = AutoDevBundle.message("chat.too.long.user.message", exceed)
        return ValidationInfo(errorMessage, this as JComponent).asWarning()
    }

    fun hasSelectedAgent(): Boolean {
        if (!project.customizeSetting.enableCustomAgent) return false
        if (customAgent.selectedItem == null) return false
        return customAgent.selectedItem != defaultRag
    }

    fun getSelectedAgent(): CustomAgentConfig {
        return customAgent.selectedItem as CustomAgentConfig
    }

    // Reset workspace panel when resetting agent
    fun resetAgent() {
        (customAgent.selectedItem as? CustomAgentConfig)?.let {
            it.state = CustomAgentState.START
        }

        customAgent.selectedItem = defaultRag
        input.text = ""
        workspaceFilePanel.clear()
    }

    fun moveCursorToStart() {
        runInEdt {
            input.requestFocus()
            input.caretModel.moveToOffset(0)
        }
    }

    private val maxHeight: Int
        get() {
            val decorator = UIUtil.getParentOfType(InternalDecorator::class.java, this)
            val contentManager = decorator?.contentManager ?: return JBUI.scale(200)
            return contentManager.component.height / 2
        }
}

private const val FONT_KEY = "FontFunction"

fun JComponent.mediumFontFunction() {
    font = JBFont.medium()
    val f: (JComponent) -> Unit = {
        it.font = JBFont.medium()
    }
    putClientProperty(FONT_KEY, f)
}

