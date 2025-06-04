package cc.unitmesh.devti.gui.chat.view

import cc.unitmesh.devti.AutoDevColors
import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import cc.unitmesh.devti.sketch.ui.MarkdownPreviewHighlightSketch
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JLabel
import javax.swing.JPanel

class MessageView(val project: Project, val message: String, val role: ChatRole, private var displayText: String) :
    JBPanel<MessageView>() {
    private var myList = JPanel(VerticalLayout(JBUI.scale(0)))
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
        initializePreAllocatedBlocks(project)

        isDoubleBuffered = true
        isOpaque = true

        layout = BorderLayout(0, 0)

        val centerPanel = JPanel(BorderLayout())

        val toolbarPanel = createToolbar()
        centerPanel.add(toolbarPanel, BorderLayout.NORTH)

        if (role == ChatRole.User) {
            var bg = AutoDevColors.USER_ROLE_BG

            runInEdt {
                val comp = createSingleTextView(project, message, background = bg, isUser = true)
                myList.add(comp)
            }

            toolbarPanel.background = bg
            centerPanel.background = bg
        }

        centerPanel.add(myList, BorderLayout.CENTER)
        add(centerPanel, BorderLayout.CENTER)

        ApplicationManager.getApplication().invokeLater {
            this@MessageView.revalidate()
            this@MessageView.repaint()
        }
    }

    private fun createToolbar(): JPanel {
        val authorLabel = JLabel().apply {
            font = JBFont.h4()
            text = when (role) {
                ChatRole.System -> "System"
                ChatRole.Assistant -> "Assistant"
                ChatRole.User -> "User"
            }
            border = JBUI.Borders.empty(0, 10)
        }

        val actionGroup = DefaultActionGroup(createToolbarActions())
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("MessageViewToolbar", actionGroup, true).apply {
                this.targetComponent = this@MessageView
            }

        val toolbarPanel = JPanel(BorderLayout()).apply {
            add(authorLabel, BorderLayout.WEST)
            add(toolbar.component, BorderLayout.EAST)
        }

        toolbarPanel.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0)
        return toolbarPanel
    }

    private fun createToolbarActions(): List<AnAction> {
        val copyAction = object : AnAction("Copy", "Copy text", AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(displayText)
                clipboard.setContents(selection, null)
            }
        }

        return listOf(copyAction)
    }

    fun onFinish(text: String) {
        displayText = text
        runInEdt {
            blockViews.filter { it.getViewText().isNotEmpty() }.forEach {
                it.onDoneStream(text)
            }

            blockViews.filter { it.getViewText().isEmpty() }.forEach {
                myList.remove(it.getComponent())
            }
        }
    }

    fun updateContent(text: String) {
        displayText = text
        val codeFenceList = CodeFence.parseAll(text)

        runInEdt {
            codeFenceList.forEachIndexed { index, codeFence ->
                if (index < blockViews.size) {
                    var langSketch: ExtensionLangSketch? = null
                    if (codeFence.originLanguage != null && codeFence.isComplete && blockViews[index] !is ExtensionLangSketch) {
                        langSketch = LanguageSketchProvider.provide(codeFence.originLanguage)
                            ?.create(project, codeFence.text)
                    }

                    val isCanHtml = codeFence.language.displayName.lowercase() == "markdown"
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
                            updateLanguage(codeFence.language, codeFence.originLanguage)
                            updateViewText(codeFence.text, codeFence.isComplete)
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
        }
    }

    companion object {
        fun createSingleTextView(
            project: Project,
            text: String,
            language: String = "markdown",
            background: JBColor? = null,
            isUser: Boolean = false,
        ): DialogPanel {
            val codeBlockViewer = CodeHighlightSketch(project, text, CodeFence.findLanguage(language), isUser = isUser).apply {
                initEditor(text)
            }

            codeBlockViewer.editorFragment!!.setCollapsed(true)
            codeBlockViewer.editorFragment!!.updateExpandCollapseLabel()

            if (background != null) {
                codeBlockViewer.border = JBUI.Borders.empty()
                codeBlockViewer.background = background
                codeBlockViewer.editorFragment?.editor?.backgroundColor = background
            }

            val panel = panel {
                row {
                    cell(codeBlockViewer).fullWidth()
                }
            }
            return panel
        }
    }
}
