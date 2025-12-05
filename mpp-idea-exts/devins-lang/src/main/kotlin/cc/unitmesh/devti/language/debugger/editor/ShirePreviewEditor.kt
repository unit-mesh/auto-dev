package cc.unitmesh.devti.language.debugger.editor

import cc.unitmesh.devti.language.psi.DevInFile
import cc.unitmesh.devti.language.run.runner.ShireRunner
import cc.unitmesh.devti.language.run.runner.ShireRunnerContext
import cc.unitmesh.devti.sketch.ui.code.CodeHighlightSketch
import cc.unitmesh.devti.sketch.ui.code.EditorFragment
import cc.unitmesh.devti.sketch.ui.code.EditorUtil
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeListener
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants

/**
 * Display shire file render prompt and have a sample file as view
 */
open class ShirePreviewEditor(
    val project: Project,
    val virtualFile: VirtualFile,
) : UserDataHolder by UserDataHolderBase(), FileEditor {
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
    private var mainEditor = MutableStateFlow<Editor?>(null)
    private val mainPanel = JPanel(BorderLayout())
    private val visualPanel: JBScrollPane = JBScrollPane(
        mainPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    )

    private var shireRunnerContext: ShireRunnerContext? = null
    private val variablePanel = ShireVariableViewPanel(project)

    private var highlightSketch: CodeHighlightSketch? = null
    private var sampleEditor: Editor? = null
    private var language: Language? = Language.findLanguageByID("JAVA")
    private val javaHelloWorld = """
        package com.phodal.shirelang;
        
        class HelloWorld {
            public static void main(String[] args) {
                System.out.println("Hello, World");
            }
        }
    """.trimIndent()

    private var editorPanel: JPanel? = null

    init {
        val corePanel = panel {
            row {
                val label = JBLabel("Shire Preview (Experimental)").apply {
                    fontColor = UIUtil.FontColor.BRIGHTER
                    background = JBColor(0xF5F5F5, 0x2B2D30)
                    font = JBUI.Fonts.label(16.0f).asBold()
                    border = JBUI.Borders.empty(0, 16)
                    isOpaque = true
                }

                cell(label).align(Align.FILL).resizableColumn()
            }
            if (language != null) {
                row {
                    cell(JBLabel("Sample file for variable").apply {
                        fontColor = UIUtil.FontColor.BRIGHTER
                        background = JBColor(0xF5F5F5, 0x2B2D30)
                        font = JBUI.Fonts.label(14.0f).asBold()
                        border = JBUI.Borders.empty(0, 16)
                        isOpaque = true
                    }).align(Align.FILL).resizableColumn()

                    cell(JBLabel("(/shire.java)", AllIcons.Actions.Edit, SwingConstants.LEADING).also {
                        it.addMouseListener(object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent?) {
                                FileFilterPopup(project) { file ->
                                    it.text = "(${file.name})"
                                    language = (file.fileType as? LanguageFileType)?.language
                                    updatePreviewEditor(file)
                                }.show(it)
                            }
                        })

                    }).align(Align.FILL).resizableColumn()
                    button("", object : AnAction() {
                        override fun actionPerformed(event: AnActionEvent) {
                            updateDisplayedContent()
                        }
                    }).also {
                        it.component.icon = AllIcons.Actions.Refresh
                        it.component.preferredSize = JBUI.size(24, 24)
                    }
                }
                row {
                    val editor = EditorUtil.createCodeViewerEditor(
                        project,
                        text = javaHelloWorld,
                        language,
                        "HelloWorld.java",
                        this@ShirePreviewEditor
                    )

                    setSampleEditor(editor) {
                        editorPanel = JPanel(BorderLayout()).apply {
                            add(it, BorderLayout.CENTER)
                            cell(this).align(Align.FILL).resizableColumn()
                        }
                    }

                }
            }
            row {
                cell(JBLabel("Variables").apply {
                    fontColor = UIUtil.FontColor.BRIGHTER
                    background = JBColor(0xF5F5F5, 0x2B2D30)
                    font = JBUI.Fonts.label(14.0f).asBold()
                    border = JBUI.Borders.empty(0, 16)
                    isOpaque = true
                }).align(Align.FILL).resizableColumn()
            }
            row {
                cell(variablePanel).align(Align.FILL)
            }
            row {
                cell(JBLabel("Prompt (some variable may be error)").apply {
                    fontColor = UIUtil.FontColor.BRIGHTER
                    background = JBColor(0xF5F5F5, 0x2B2D30)
                    font = JBUI.Fonts.label(14.0f).asBold()
                    border = JBUI.Borders.empty(0, 16)
                    isOpaque = true
                }).align(Align.FILL).resizableColumn()
            }
            row {
                highlightSketch = CodeHighlightSketch(project, "", MarkdownLanguage.INSTANCE, 18).apply {
                    initEditor("Please refresh to see the result")
                }
                highlightSketch?.editorFragment?.setCollapsed(true)
                highlightSketch?.editorFragment?.updateExpandCollapseLabel()

                val panel = JPanel(BorderLayout())
                panel.border = BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(12, 12, 12, 12),
                    RoundedLineBorder(JBColor.border(), 8, 1)
                )
                highlightSketch?.let { panel.add(it, BorderLayout.CENTER) }

                cell(panel).align(Align.FILL)
            }
        }

        this.mainPanel.add(corePanel, BorderLayout.CENTER)
    }

    fun updateDisplayedContent() {
        try {
            AutoDevCoroutineScope.scope(project).launch {
                runBlocking {
                    val psiFile = smartReadAction(project) {
                        PsiManager.getInstance(project).findFile(virtualFile) as? DevInFile
                    } ?: return@runBlocking

                    shireRunnerContext = ShireRunner.compileOnly(project, psiFile, mapOf(), sampleEditor)

                    val variables = shireRunnerContext?.compiledVariables
                    if (variables != null) {
                        variablePanel.updateVariables(variables)
                    }

                    highlightSketch?.updateViewText(shireRunnerContext!!.finalPrompt, false)
                    highlightSketch?.repaint()

                    mainPanel.revalidate()
                    mainPanel.repaint()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setMainEditor(editor: Editor) {
        check(mainEditor.value == null)
        mainEditor.value = editor
    }

    fun scrollToSrcOffset(offset: Int) {
        val highlightEditor = highlightSketch?.editorFragment?.editor
        if (highlightEditor == null) {
            visualPanel.verticalScrollBar.value = offset
            return
        }

        val position = highlightEditor.offsetToLogicalPosition(offset)
        highlightEditor.scrollingModel.scrollTo(position, ScrollType.MAKE_VISIBLE)
    }

    private fun updatePreviewEditor(file: VirtualFile) {
        FileDocumentManager.getInstance().getDocument(file)?.text?.let { text ->

            val language = language ?: CodeFence.findLanguage("Plain text")
            val lightFile = object : LightVirtualFile(file.name, language, text) {
                override fun getPath() = file.path
            }

            val document = FileDocumentManager.getInstance().getDocument(lightFile) ?: return@let

            val editor = EditorUtil.createCodeViewerEditor(
                project, lightFile, document, this
            )

            setSampleEditor(editor) {
                editorPanel?.removeAll()
                editorPanel?.add(it, BorderLayout.CENTER)
            }
            updateDisplayedContent()
        }
    }

    private fun setSampleEditor(editor: EditorEx, consume: (JComponent) -> Unit) {
        editor.isViewer = false
        editor.settings.isLineNumbersShown = true

        val editorFragment = EditorFragment(editor)
        editorFragment.setCollapsed(true)
        editorFragment.updateExpandCollapseLabel()

        sampleEditor = editor

        consume(editorFragment.getContent())
    }

    override fun getComponent(): JComponent = visualPanel
    override fun getName(): String = "Shire Prompt Preview"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun getFile(): VirtualFile = virtualFile
    override fun getPreferredFocusedComponent(): JComponent? = null
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {}
}