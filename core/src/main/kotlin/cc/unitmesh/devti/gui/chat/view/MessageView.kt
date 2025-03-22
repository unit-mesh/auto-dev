package cc.unitmesh.devti.gui.chat.view

import cc.unitmesh.devti.AutoDevIcons
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
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
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

        val authorLabel = JLabel()
        authorLabel.setFont(JBFont.h4())
        authorLabel.setText(
            when (role) {
                ChatRole.System -> "System"
                ChatRole.Assistant -> "Assistant"
                ChatRole.User -> "User"
            }
        )

        layout = BorderLayout(JBUI.scale(4), 0)

        val centerPanel = JPanel(VerticalLayout(JBUI.scale(4)))
        centerPanel.add(authorLabel)

        val toolbar = createViewActionGroup().component
        runInEdt {
            centerPanel.add(toolbar)
            if (role == ChatRole.User) {
                myList.add(createSingleTextView(project, message))
            }
        }

        centerPanel.add(myList)
        add(centerPanel, BorderLayout.CENTER)

        ApplicationManager.getApplication().invokeLater {
            this@MessageView.revalidate()
            this@MessageView.repaint()
        }
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


    fun createViewActionGroup(): ActionToolbar {
        val copyAction = object : AnAction("Copy", "Copy text", AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(displayText)
                clipboard.setContents(selection, null)
            }
        }

        val actionGroup = DefaultActionGroup(listOf(copyAction))
        val rightToolbar = ActionManager.getInstance()
            .createActionToolbar("AutoDevCopyView", actionGroup, true)

        rightToolbar.targetComponent = this
        return rightToolbar
    }

    companion object {
        fun createSingleTextView(project: Project, text: String, language: String = "markdown"): DialogPanel {
            val codeBlockViewer = CodeHighlightSketch(project, text, CodeFence.findLanguage(language)).apply {
                initEditor(text)
            }

            codeBlockViewer.editorFragment!!.setCollapsed(true)
            codeBlockViewer.editorFragment!!.updateExpandCollapseLabel()

            val panel = panel {
                row {
                    cell(codeBlockViewer).fullWidth()
                }
            }
            return panel
        }
    }
}
