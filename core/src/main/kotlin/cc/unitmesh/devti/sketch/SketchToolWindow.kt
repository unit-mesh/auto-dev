package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.alignRight
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputSection
import cc.unitmesh.devti.gui.chat.view.MessageView
import cc.unitmesh.devti.gui.toolbar.CopyAllMessagesAction
import cc.unitmesh.devti.gui.toolbar.NewSketchAction
import cc.unitmesh.devti.gui.toolbar.SummaryMessagesAction
import cc.unitmesh.devti.inline.AutoDevInlineChatService
import cc.unitmesh.devti.inline.fullHeight
import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.MarkdownPreviewHighlightSketch
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

interface SketchProcessListener {
    fun onBefore() {}
    fun onAfter() {}
}

open class SketchToolWindow(
    val project: Project,
    open val editor: Editor?,
    showInput: Boolean = false,
    chatActionType: ChatActionType = ChatActionType.SKETCH
) : SimpleToolWindowPanel(true, true), NullableComponent, Disposable {
    open val chatCodingService = ChatCodingService(chatActionType, project)
    open val inputListener: SketchInputListener = SketchInputListener(project, chatCodingService, this)
    private var progressBar: JProgressBar = JProgressBar()
    private val renderInWebview = project.coderSetting.state.enableRenderWebview
    private val isAutoScroll = project.coderSetting.state.enableAutoScrollInSketch

    private var thinkingHighlight: CodeHighlightSketch =
        CodeHighlightSketch(project, "<Thinking />", PlainTextLanguage.INSTANCE, withLeftRightBorder = false)

    private var thinkingScrollPane = JBScrollPane(thinkingHighlight).apply {
        verticalScrollBar.unitIncrement = 16
        preferredSize = JBUI.size(Int.MAX_VALUE, JBUI.scale(250)) // Limit height to 100
        maximumSize = JBUI.size(Int.MAX_VALUE, JBUI.scale(250))  // Enforce maximum height
    }
    
    private var thinkingPanel = JPanel(BorderLayout()).apply {
        add(thinkingScrollPane, BorderLayout.CENTER)
        isVisible = false
    }

    private var inputSection: AutoDevInputSection = AutoDevInputSection(project, this, showAgent = false)

    private var myText: String = ""

    private var myList = JPanel(VerticalLayout(JBUI.scale(0))).apply {
        this.isOpaque = true
    }
    private var historyPanel = JPanel(VerticalLayout(JBUI.scale(0))).apply {
        this.isOpaque = true
    }

    var isUserScrolling: Boolean = false
    protected var isInterrupted: Boolean = false

    protected var systemPromptPanel: JPanel = JPanel(BorderLayout())
    protected var contentPanel = JPanel(BorderLayout())

    protected var panelContent: DialogPanel = panel {
        row { cell(systemPromptPanel).fullWidth().fullHeight() }
        row { cell(historyPanel).fullWidth().fullHeight() }
        row { cell(myList).fullWidth().fullHeight() }
    }

    private val scrollPanel: JBScrollPane = JBScrollPane(
        panelContent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ).apply {
        this.verticalScrollBar.autoscrolls = isAutoScroll
        this.verticalScrollBar.addAdjustmentListener { e ->
            if (e.valueIsAdjusting) {
                isUserScrolling = true
                this.verticalScrollBar.autoscrolls = false
            }
        }
    }

    var handleCancel: ((String) -> Unit)? = null

    private val processListeners = mutableListOf<SketchProcessListener>()
    private val blockViews: MutableList<LangSketch> = mutableListOf()
    private fun initializePreAllocatedBlocks(project: Project) {
        repeat(32) {
            runInEdt {
                val codeBlockViewer = CodeHighlightSketch(project, "", PlainTextLanguage.INSTANCE)
                blockViews.add(codeBlockViewer)
                myList.add(codeBlockViewer)
            }
        }
    }

    init {
        if (showInput) {
            val header = panel {
                row {
                    checkBox(AutoDevBundle.message("sketch.composer.mode")).apply {
                        this.component.addActionListener {
                            AutoSketchMode.getInstance(project).isEnable = this.component.isSelected
                        }

                        val connection = ApplicationManager.getApplication().messageBus.connect(this@SketchToolWindow)
                        connection.subscribe(AutoSketchModeListener.TOPIC, object : AutoSketchModeListener {
                            override fun start() {
                                this@apply.component.isSelected = true
                            }

                            override fun done() {
                                // do nothing
                            }
                        })

                    }

                    val buttonBox = Box.createHorizontalBox()
                    buttonBox.add(Box.createHorizontalGlue())
                    buttonBox.add(createActionButton(NewSketchAction()))
                    buttonBox.add(createActionButton(CopyAllMessagesAction()))
                    buttonBox.add(createActionButton(SummaryMessagesAction()))
                    cell(buttonBox).alignRight()
                }
            }

            header.border = JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(0, 4)
            )

            contentPanel.add(header, BorderLayout.NORTH)
        }

        contentPanel.add(scrollPanel, BorderLayout.CENTER)
        contentPanel.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    if (editor != null) {
                        AutoDevInlineChatService.getInstance().closeInlineChat(editor!!)
                    }
                }
            }
        })

        if (showInput) {
            ApplicationManager.getApplication().invokeLater {
                AutoDevCoroutineScope.workerScope(project).launch {
                    setupListener()
                }
            }
        }

        setContent(contentPanel)
        initializePreAllocatedBlocks(project)
    }

    private suspend fun setupListener() {
        inputSection.also {
            it.border = JBUI.Borders.empty(8)
        }

        inputListener.setup()
        inputSection.addListener(inputListener)

        runInEdt {
            contentPanel.add(panel {
                row {
                    cell(progressBar).fullWidth()
                }
                row {
                    cell(thinkingPanel).fullWidth()
                }
                row {
                    cell(inputSection).fullWidth()
                }
            }, BorderLayout.SOUTH)
        }

        addProcessListener(object : SketchProcessListener {
            override fun onBefore() {
                isInterrupted = false
                inputSection.showStopButton()
            }

            override fun onAfter() {
                inputSection.showSendButton()
            }
        })
    }

    fun onStart() {
        beforeRun()
        progressBar.isVisible = true
    }

    fun hiddenProgressBar() {
        progressBar.isVisible = false
    }

    fun stop() {
        cancel("Stop")
        inputSection.showSendButton()
    }

    fun addProcessListener(processorListener: SketchProcessListener) {
        processListeners.add(processorListener)
    }

    fun beforeRun() {
        processListeners.forEach { it.onBefore() }
    }

    fun afterRun() {
        processListeners.forEach { it.onAfter() }
    }

    override fun dispose() {
        chatCodingService.clearSession()
    }

    fun addRequestPrompt(text: String) {
        progressBar.isIndeterminate = true

        runInEdt {
            historyPanel.add(createSingleTextView(text, language = "DevIn"))
            this.revalidate()
            this.repaint()
        }
    }

    fun addSystemPrompt(text: String) {
        runInEdt {
            systemPromptPanel.add(createSingleTextView(text, language = "VTL"))
            this.revalidate()
            this.repaint()
        }
    }

//    fun clearSystemPromptPanel() {
//        systemPromptPanel.removeAll()
//        systemPromptPanel.revalidate()
//        systemPromptPanel.repaint()
//    }

    fun updateHistoryPanel() {
        runInEdt {
            blockViews.filter { it.getViewText().isNotEmpty() }.forEach {
                historyPanel.add(it.getComponent())
            }

            blockViews.clear()
            myList.removeAll()

            this.revalidate()
            this.repaint()
        }
    }

    fun setInput(text: String) {
        inputSection.setText(text.trim())
    }

    fun createSingleTextView(text: String, language: String = "markdown"): DialogPanel {
        return MessageView.createSingleTextView(project, text, language)
    }

    fun onUpdate(text: String) {
        myText = text
        val codeFenceList = CodeFence.parseAll(text)

        runInEdt {
            codeFenceList.forEachIndexed { index, codeFence ->
                if (index < blockViews.size) {
                    var langSketch: ExtensionLangSketch? = null
                    if (codeFence.originLanguage != null && codeFence.isComplete && blockViews[index] !is ExtensionLangSketch) {
                        langSketch = LanguageSketchProvider.provide(codeFence.originLanguage)
                            ?.create(project, codeFence.text)

                        langSketch?.onComplete(codeFence.text)
                    }

                    val isCanHtml = renderInWebview && codeFence.language.displayName.lowercase() == "markdown"
                    if (isCanHtml && codeFence.isComplete && blockViews[index] !is ExtensionLangSketch) {
                        langSketch = MarkdownPreviewHighlightSketch(project, codeFence.text)
                    }

                    if (langSketch != null) {
                        val oldComponent = blockViews[index]
                        blockViews[index] = langSketch
                        myList.remove(index)
                        myList.add(langSketch.getComponent(), index)

                        oldComponent.dispose()
                    } else {
                        blockViews[index].apply {
                            var originLanguage = codeFence.originLanguage
                            if (originLanguage == "DevIn" && codeFence.isComplete && this.hasRenderView() == false) {
                                createRenderSketch(codeFence)?.also {
                                    this@apply.addOrUpdateRenderView(it)
                                }
                            }

                            var language = codeFence.language
                            updateLanguage(language, originLanguage)
                            updateViewText(codeFence.text, codeFence.isComplete)
                        }
                    }
                } else {
                    val codeBlockViewer = CodeHighlightSketch(project, "", PlainTextLanguage.INSTANCE)
                    blockViews.add(codeBlockViewer)
                    myList.add(codeBlockViewer.getComponent())
                }
            }

            while (blockViews.size > codeFenceList.size) {
                val lastIndex = blockViews.lastIndex
                try {
                    blockViews.removeAt(lastIndex)
                    myList.remove(lastIndex)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            myList.revalidate()
            myList.repaint()
        }

        scrollToBottom()
    }

    fun createRenderSketch(code: CodeFence): JComponent? {
        val codes = CodeFence.parseAll(code.text)

        val panels = codes.map { code ->
            var panel: JComponent = when (code.originLanguage) {
                "diff", "patch" -> {
                    val langSketch = LanguageSketchProvider.provide("patch")?.create(project, code.text)
                        ?: return@map null
                    langSketch.onComplete(code.text)
                    langSketch.getComponent()
                }

                "html" -> {
                    val langSketch = LanguageSketchProvider.provide("html")?.create(project, code.text)
                        ?: return@map null
                    langSketch.onComplete(code.text)
                    langSketch.getComponent()
                }

                "bash", "shell" -> {
                    val langSketch = LanguageSketchProvider.provide("shell")?.create(project, code.text)
                        ?: return@map null
                    langSketch.onComplete(code.text)
                    langSketch.getComponent()
                }

                else -> null
            } ?: return@map null

            panel.border = JBEmptyBorder(0, 4, 0, 4)
            panel
        }.filterNotNull()

        if (panels.isEmpty()) return null

        val blockedPanel = JPanel(VerticalLayout(JBUI.scale(0))).apply {
            this.isOpaque = true
            this.border = JBEmptyBorder(0, 4, 0, 4)
            panels.forEach { this.add(it) }
        }

        return blockedPanel
    }

    fun onFinish(text: String) {
        myText = text
        runInEdt {
            blockViews.filter { it.getViewText().isNotEmpty() }.forEach {
                it.onDoneStream(text)
            }

            blockViews.filter { it.getViewText().isEmpty() }.forEach {
                myList.remove(it.getComponent())
            }
        }

        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        scrollToBottom(force = true)

        afterRun()

        if (AutoSketchMode.getInstance(project).isEnable && !isInterrupted) {
            AutoDevCoroutineScope.scope(project).launch {
                AutoSketchMode.getInstance(project).start(text, this@SketchToolWindow.inputListener)
            }
        }

        this.revalidate()
        this.repaint()
    }

    fun sendInput(text: String) {
        inputSection.setText(text.trim())
        ApplicationManager.getApplication().invokeLater {
            inputSection.send()
        }
    }

    fun putText(text: String) {
        runInEdt {
            inputSection.setText(text.trim())
        }
    }

    fun scrollToBottom(force: Boolean = false) {
        if ((!isUserScrolling && isAutoScroll) || force == true) {
            ApplicationManager.getApplication().invokeLater {
                val verticalScrollBar = scrollPanel.verticalScrollBar
                verticalScrollBar.value = verticalScrollBar.maximum
            }
        }
    }

    fun resize(maxHeight: Int = 480) {
        val height = myList.components.sumOf { it.height }
        if (height < maxHeight) {
            this.minimumSize = JBUI.size(800, height)
        } else {
            this.minimumSize = JBUI.size(800, maxHeight)
            scrollPanel.minimumSize = JBUI.size(800, maxHeight)
        }
    }

    override fun isNull(): Boolean = !isVisible

    fun cancel(s: String) = runCatching {
        handleCancel?.also { handleCancel = null }?.invoke(s)
        isInterrupted = true
    }

    fun resetSketchSession() {
        chatCodingService.clearSession()
        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        blockViews.clear()
        systemPromptPanel.removeAll()
        myList.removeAll()
        historyPanel.removeAll()
        initializePreAllocatedBlocks(project)

        project.getService(AgentStateService::class.java).resetState()
    }

    fun printThinking(string: String) {
        runInEdt {
            thinkingPanel.isVisible = true
            thinkingHighlight.updateViewText(string, false)
            SwingUtilities.invokeLater {
                thinkingScrollPane.verticalScrollBar.value = thinkingScrollPane.verticalScrollBar.maximum
            }
        }
    }

    fun hiddenThinking() {
        runInEdt {
            thinkingPanel.isVisible = false
        }
    }
}

fun Row.createActionButton(action: AnAction, @NonNls actionPlace: String = ActionPlaces.UNKNOWN): ActionButton {
    val component = ActionButton(
        action,
        action.templatePresentation.clone(),
        actionPlace,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )
    return component
}
