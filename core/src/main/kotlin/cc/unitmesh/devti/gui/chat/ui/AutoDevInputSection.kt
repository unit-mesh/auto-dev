package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.agent.configurable.customAgentSetting
import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.agent.model.CustomAgentState
import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.llms.tokenizer.TokenizerFactory
import cc.unitmesh.devti.provider.RelatedClassesProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.util.isInProject
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.diff.editor.DiffVirtualFileBase
import com.intellij.icons.AllIcons
import com.intellij.ide.IdeTooltip
import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.openapi.vfs.VirtualFile
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
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBEmptyBorder
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

data class ModelWrapper(val virtualFile: VirtualFile, var panel: JPanel? = null, var namePanel: JPanel? = null)

class AutoDevInputSection(private val project: Project, val disposable: Disposable?, showAgent: Boolean = true) :
    BorderLayoutPanel() {
    private val input: AutoDevInput
    private val documentListener: DocumentListener
    private val sendButtonPresentation: Presentation
    private val stopButtonPresentation: Presentation
    private val sendButton: ActionButton
    private val stopButton: ActionButton
    private val buttonPanel = JPanel(CardLayout())
    private val inputPanel = BorderLayoutPanel()

    private val listModel = DefaultListModel<ModelWrapper>()
    private val elementsList = JBList(listModel)

    private val defaultRag: CustomAgentConfig = CustomAgentConfig("<Select Custom Agent>", "Normal")
    private var customRag: ComboBox<CustomAgentConfig> = ComboBox(MutableCollectionComboBoxModel(listOf()))

    private val logger = logger<AutoDevInputSection>()

    val editorListeners = EventDispatcher.create(AutoDevInputListener::class.java)
    private var tokenizer: Tokenizer? = try {
        lazy { TokenizerFactory.createTokenizer() }.value
    } catch (e: Exception) {
        logger.error("TokenizerImpl.INSTANCE is not available", e)
        null
    }

    var text: String
        get() {
            return input.text
        }
        set(text) {
            input.recreateDocument()
            input.text = text
        }

    init {
        input = AutoDevInput(project, listOf(), disposable, this)

        setupElementsList()
        val sendButtonPresentation = Presentation(AutoDevBundle.message("chat.panel.send"))
        sendButtonPresentation.icon = AutoDevIcons.Send
        this.sendButtonPresentation = sendButtonPresentation

        val stopButtonPresentation = Presentation("Stop")
        stopButtonPresentation.icon = AutoDevIcons.Stop
        this.stopButtonPresentation = stopButtonPresentation

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

        if (project.customAgentSetting.enableCustomRag && showAgent) {
            customRag = ComboBox(MutableCollectionComboBoxModel(loadRagApps()))
            customRag.renderer = SimpleListCellRenderer.create { label: JBLabel, value: CustomAgentConfig?, _: Int ->
                if (value != null) {
                    label.text = value.name
                }
            }
            customRag.selectedItem = defaultRag

            input.minimumSize = Dimension(input.minimumSize.width, 64)
            layoutPanel.addToLeft(customRag)
        }


        buttonPanel.add(sendButton, "Send")
        buttonPanel.add(stopButton, "Stop")

        layoutPanel.addToCenter(horizontalGlue)
        layoutPanel.addToRight(buttonPanel)

        inputPanel.add(input, BorderLayout.CENTER)
        inputPanel.addToBottom(layoutPanel)

        this.add(elementsList, BorderLayout.NORTH)
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

        /// get current open file and add to the list
        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        currentFile?.let {
            listModel.addIfAbsent(currentFile, first = true)
        }
    }

    private fun setupEditorListener() {
        project.messageBus.connect(disposable!!).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file = event.newFile ?: return
                    ApplicationManager.getApplication().invokeLater {
                        listModel.addIfAbsent(file, true)
                    }
                }
            }
        )
    }

    private fun setupRelatedListener() {
        project.messageBus.connect(disposable!!)
            .subscribe(LookupManagerListener.TOPIC, AutoDevInputLookupManagerListener(project) {
                ApplicationManager.getApplication().invokeLater {
                    val relatedElements = RelatedClassesProvider.provide(it.language)?.lookup(it)
                    updateElements(relatedElements)
                }
            })
    }

    private fun setupElementsList() {
        elementsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        elementsList.layoutOrientation = JList.HORIZONTAL_WRAP
        elementsList.visibleRowCount = 2
        elementsList.cellRenderer = RelatedFileListCellRenderer()
        elementsList.setEmptyText("")

        elementsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val list = e.source as JBList<*>
                val index = list.locationToIndex(e.point)
                if (index == -1) return

                val wrapper = listModel.getElementAt(index)
                val cellBounds = list.getCellBounds(index, index)
                wrapper.panel?.components?.firstOrNull { it.contains(e.x - cellBounds.x - it.x, it.height - 1) }
                    ?.let { component ->
                        when {
                            component is JPanel -> {
                                listModel.removeElement(wrapper)
                                val vfile = wrapper.virtualFile
                                val relativePath = vfile.path.substringAfter(project.basePath!!).removePrefix("/")
                                listModel.addIfAbsent(vfile)

                                input.appendText("\n/" + "file" + ":${relativePath}")
                                listModel.indexOf(wrapper.virtualFile).takeIf { it != -1 }
                                    ?.let { listModel.remove(it) }

                                // invoake later
                                ApplicationManager.getApplication().invokeLater {
                                    val psiFile = PsiManager.getInstance(project).findFile(vfile) ?: return@invokeLater
                                    val relatedElements =
                                        RelatedClassesProvider.provide(psiFile.language)?.lookup(psiFile)
                                    updateElements(relatedElements)
                                }
                            }

                            component is JLabel && component.icon == AllIcons.Actions.Close -> listModel.removeElement(
                                wrapper
                            )

                            else -> list.clearSelection()
                        }
                    } ?: list.clearSelection()
            }
        })
    }

    private fun updateElements(elements: List<PsiElement>?) {
        elements?.forEach { listModel.addIfAbsent(it.containingFile.virtualFile) }
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
        val result = super.getPreferredSize()
        result.height = max(min(result.height, maxHeight), minimumSize.height)
        return result
    }

    fun selectAgent(config: CustomAgentConfig) {
        customRag.selectedItem = config
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
        if (!project.customAgentSetting.enableCustomRag) return false
        if (customRag.selectedItem == null) return false
        return customRag.selectedItem != defaultRag
    }

    fun getSelectedAgent(): CustomAgentConfig {
        return customRag.selectedItem as CustomAgentConfig
    }

    fun resetAgent() {
        (customRag.selectedItem as? CustomAgentConfig)?.let {
            it.state = CustomAgentState.START
        }

        customRag.selectedItem = defaultRag
        text = ""
    }

    fun moveCursorToStart() {
        input.requestFocus()
        input.caretModel.moveToOffset(0)
    }

    private val maxHeight: Int
        get() {
            val decorator = UIUtil.getParentOfType(InternalDecorator::class.java, this)
            val contentManager = decorator?.contentManager ?: return JBUI.scale(200)
            return contentManager.component.height / 2
        }

    val focusableComponent: JComponent get() = input

    private fun DefaultListModel<ModelWrapper>.addIfAbsent(vfile: VirtualFile, first: Boolean = false) {
        if (!isInProject(vfile, project)) return
        if (vfile is DiffVirtualFileBase) return

        if (elements().asSequence().none { it.virtualFile == vfile }) {
            val modelWrapper = ModelWrapper(vfile)
            if (first) {
                insertElementAt(modelWrapper, 0)
            } else {
                addElement(modelWrapper)
            }
        }
    }
}
