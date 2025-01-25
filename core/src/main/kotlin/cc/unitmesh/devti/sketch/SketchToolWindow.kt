package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.alignRight
import cc.unitmesh.devti.gui.chat.ChatCodingService
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputSection
import cc.unitmesh.devti.inline.AutoDevInlineChatService
import cc.unitmesh.devti.inline.fullHeight
import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class SketchToolWindow(val project: Project, val editor: Editor?, private val showInput: Boolean = false) :
    SimpleToolWindowPanel(true, true),
    NullableComponent, Disposable {
    private val chatCodingService = ChatCodingService(ChatActionType.SKETCH, project)
    private var progressBar: CustomProgressBar = CustomProgressBar(this)
    private var shireInput: AutoDevInputSection = AutoDevInputSection(project, this, showAgent = false)

    private var myText: String = ""

    private var myList = JPanel(VerticalLayout(JBUI.scale(0))).apply {
        this.isOpaque = true
    }
    private var isUserScrolling: Boolean = false

    private var userPrompt: JPanel = JPanel(BorderLayout()).apply {
        this.isOpaque = true
        this.border = JBUI.Borders.empty(10, 0)
    }

    private var contentPanel = JPanel(BorderLayout()).apply {
        this.isOpaque = true
    }

    val header = JButton(AllIcons.Actions.Copy).apply {
        this.border = JBUI.Borders.empty(10, 20)
        this.isOpaque = true
        this.preferredSize = Dimension(32, 32)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                val selection = StringSelection(myText)
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(selection, null)
            }
        })
    }

    private var panelContent: DialogPanel = panel {
        if (showInput) {
            row {
                checkBox(AutoDevBundle.message("sketch.composer.mode")).apply {
                    this.component.addActionListener {
                        AutoSketchMode.getInstance(project).isEnable = this.component.isSelected
                    }
                }
            }
        }
        row { cell(userPrompt).fullWidth().fullHeight() }
        row {
            cell(header).alignRight()
        }
        row { cell(myList).fullWidth().fullHeight() }
    }

    private val scrollPanel: JBScrollPane = JBScrollPane(
        panelContent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ).apply {
        this.verticalScrollBar.autoscrolls = true
        this.verticalScrollBar.addAdjustmentListener { e ->
            if (e.valueIsAdjusting) {
                isUserScrolling = true
            }
        }
    }

    var handleCancel: ((String) -> Unit)? = null

    private val listener = SketchInputListener(project, chatCodingService, this)

    init {
        contentPanel.add(scrollPanel, BorderLayout.CENTER)
        contentPanel.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    if (editor != null) {
                        AutoDevInlineChatService.getInstance().closeInlineChat(editor)
                    }
                }
            }
        })

        contentPanel.add(progressBar, BorderLayout.SOUTH)

        if (showInput) {
            shireInput.also {
                border = JBUI.Borders.empty(8)
            }

            shireInput.addListener(listener)
            contentPanel.add(shireInput, BorderLayout.SOUTH)
        }

        setContent(contentPanel)
    }

    fun onStart() {
        initializePreAllocatedBlocks(project)
        progressBar.isIndeterminate = true
        progressBar.isVisible = !showInput
    }

    fun hiddenProgressBar() {
        progressBar.isVisible = false
    }

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

    override fun dispose() {
        chatCodingService.clearSession()
    }

    fun addRequestPrompt(text: String) {
        runInEdt {
            val codeBlockViewer = CodeHighlightSketch(project, text, PlainTextLanguage.INSTANCE).apply {
                initEditor(text)
            }

            codeBlockViewer.editorFragment!!.setCollapsed(true)
            codeBlockViewer.editorFragment!!.updateExpandCollapseLabel()

            val panel = panel {
                row {
                    cell(codeBlockViewer).fullWidth()
                }
            }.also {
                it.border = JBUI.Borders.empty(10, 0)
            }

            userPrompt.removeAll()
            userPrompt.add(panel, BorderLayout.CENTER)

            this.revalidate()
            this.repaint()
        }
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
                    }

                    if (langSketch != null) {
                        val oldComponent = blockViews[index]
                        blockViews[index] = langSketch
                        myList.remove(index)
                        myList.add(langSketch.getComponent(), index)

                        oldComponent.dispose()
                    } else {
                        blockViews[index].apply {
                            updateLanguage(codeFence.language, codeFence.originLanguage)
                            updateViewText(codeFence.text)
                        }
                    }
                } else {
                    val codeBlockViewer = CodeHighlightSketch(project, codeFence.text, PlainTextLanguage.INSTANCE)
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

            scrollToBottom()
        }
    }

    fun onFinish(text: String) {
        myText = text
        runInEdt {
            blockViews.filter { it.getViewText().isNotEmpty() }.forEach {
                it.doneUpdateText(text)
            }

            blockViews.filter { it.getViewText().isEmpty() }.forEach {
                myList.remove(it.getComponent())
            }
        }

        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        scrollToBottom()

        if (AutoSketchMode.getInstance(project).isEnable) {
            AutoSketchMode.getInstance(project).start(text, this@SketchToolWindow.listener)
        }
    }

    fun sendInput(text: String) {
        shireInput.text += "\n" + text
        shireInput.send()
    }

    private fun scrollToBottom() {
        if (!isUserScrolling) {
            SwingUtilities.invokeLater {
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

    fun cancel(s: String) = runCatching { handleCancel?.invoke(s) }

    fun resetSketchSession() {
        chatCodingService.clearSession()
        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        userPrompt.removeAll()
        myList.removeAll()
        initializePreAllocatedBlocks(project)
    }
}

class CustomProgressBar(private val view: SketchToolWindow) : JPanel(BorderLayout()) {
    private val progressBar: JProgressBar = JProgressBar()

    var isIndeterminate = progressBar.isIndeterminate
        set(value) {
            progressBar.isIndeterminate = value
            field = value
        }

    private val cancelLabel = JBLabel(AllIcons.Actions.CloseHovered)

    init {
        cancelLabel.setBorder(JBUI.Borders.empty(0, 5))
        cancelLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                view.cancel("This progressBar is canceled")
            }
        })

        add(progressBar, BorderLayout.CENTER)
        add(cancelLabel, BorderLayout.EAST)
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        progressBar.isVisible = visible
        cancelLabel.isVisible = visible
    }
}