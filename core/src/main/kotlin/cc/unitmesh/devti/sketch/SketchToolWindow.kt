package cc.unitmesh.devti.sketch

import cc.unitmesh.devti.gui.chat.*
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputSection
import cc.unitmesh.devti.inline.AutoDevInlineChatService
import cc.unitmesh.devti.inline.fullHeight
import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.util.parser.CodeFence
import cc.unitmesh.devti.sketch.highlight.CodeHighlightSketch
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
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

class SketchToolWindow(val project: Project, val editor: Editor?, private val showInput: Boolean = false) :
    SimpleToolWindowPanel(true, true),
    NullableComponent, Disposable {
    private var progressBar: CustomProgressBar = CustomProgressBar(this)
    private var shireInput: AutoDevInputSection = AutoDevInputSection(project, this, showAgent = false)

    private var myList = JPanel(VerticalLayout(JBUI.scale(0))).apply {
        this.isOpaque = true
    }

    private var userPrompt: JPanel = JPanel(BorderLayout()).apply {
        this.isOpaque = true
        this.border = JBUI.Borders.empty(10, 0)
    }

    private var contentPanel = JPanel(BorderLayout()).apply {
        this.isOpaque = true
    }

    private var panelContent: DialogPanel = panel {
        row { cell(progressBar).fullWidth() }
        row { cell(userPrompt).fullWidth().fullHeight() }
        row { cell(myList).fullWidth().fullHeight() }
    }

    private val scrollPanel: JBScrollPane = JBScrollPane(
        panelContent,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    ).apply {
        this.verticalScrollBar.autoscrolls = true
    }

    var handleCancel: ((String) -> Unit)? = null

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

        if (showInput) {
            shireInput.also {
                border = JBUI.Borders.empty(8)
            }

            val chatCodingService = ChatCodingService(ChatActionType.SKETCH, project)
            shireInput.addListener(SketchAutoDevInputListener(project, chatCodingService, this))
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
        repeat(16) {
            runInEdt {
                val codeBlockViewer = CodeHighlightSketch(project, "", PlainTextLanguage.INSTANCE)
                blockViews.add(codeBlockViewer)
                myList.add(codeBlockViewer)
            }
        }
    }

    override fun dispose() {
    }

    fun addRequestPrompt(text: String) {
        runInEdt {
            val codeBlockViewer = CodeHighlightSketch(project, text, CodeFence.findLanguage("Markdown")).apply {
                initEditor(text)
            }

            codeBlockViewer.editorFragment?.setCollapsed(true)
            codeBlockViewer.editorFragment!!.updateExpandCollapseLabel()

            val panel = panel {
                row {
                    cell(codeBlockViewer).fullWidth()
                }
            }.also {
                it.border = JBUI.Borders.empty(10, 0)
            }

            userPrompt.add(panel, BorderLayout.CENTER)

            this.revalidate()
            this.repaint()
        }
    }

    fun onUpdate(text: String) {
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
                blockViews.removeAt(lastIndex)
                myList.remove(lastIndex)
            }

            myList.revalidate()
            myList.repaint()

            scrollToBottom()
        }
    }

    fun onFinish(text: String) {
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
    }

    private fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val verticalScrollBar = scrollPanel.verticalScrollBar
            verticalScrollBar.value = verticalScrollBar.maximum
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

    override fun isNull(): Boolean {
        return !isVisible
    }

    fun cancel(s: String) = runCatching { handleCancel?.invoke(s) }
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