// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.messages.Topic
import com.intellij.util.ui.JBUI
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

class CodeBlockView(
    private val block: CodeBlock,
    private val project: Project,
    private val disposable: Disposable,
) :
    MessageBlockView {
    private var editorInfo: CodePartEditorInfo? = null

    init {
        block.addTextListener {
            if (editorInfo == null) return@addTextListener
            updateOrCreateCodeView()
        }
    }

    override fun getBlock(): CodeBlock {
        return block
    }

    override fun getComponent(): JComponent {
        return editorInfo?.component ?: return updateOrCreateCodeView()!!.component
    }

    val codeContent: String
        get() {
            return editorInfo?.code?.get() ?: ""
        }

    override fun initialize() {
        if (editorInfo == null) {
            updateOrCreateCodeView()
        }
    }

    private fun updateOrCreateCodeView(): CodePartEditorInfo? {
        val code: CodeFence = getBlock().code
        if (editorInfo == null) {
            val graphProperty = PropertyGraph(null, false).property(code.text)
            val editorInfo: CodePartEditorInfo = createCodeViewer(
                project, graphProperty, disposable, code.language, getBlock().getMessage()
            )
            this.editorInfo = editorInfo
        } else {
            val codePartEditorInfo = editorInfo
            if (codePartEditorInfo!!.language == code.language) {
                editorInfo!!.language = code.language
            }
            ApplicationManager.getApplication().runWriteAction{
                editorInfo!!.editor.document.setText(code.text)
            }
            editorInfo!!.code.set(code.text)
        }

        return editorInfo
    }

    companion object {
        private fun createCodeViewerEditor(
            project: Project,
            file: LightVirtualFile,
            document: Document,
            disposable: Disposable,
        ): EditorEx {
            val editor: EditorEx = ReadAction.compute<EditorEx, Throwable> {
                EditorFactory.getInstance()
                    .createViewer(document, project, EditorKind.PREVIEW) as EditorEx
            }

            disposable.whenDisposed(disposable) {
                EditorFactory.getInstance().releaseEditor(editor)
            }

            editor.setFile(file)
            editor.setCaretEnabled(true)
            val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file)

            editor.highlighter = highlighter

            val markupModel: MarkupModelEx = editor.markupModel
            (markupModel as EditorMarkupModel).isErrorStripeVisible = false

            val settings = editor.settings.also {
                it.isDndEnabled = false
                it.isLineNumbersShown = false
                it.additionalLinesCount = 0
                it.isLineMarkerAreaShown = false
                it.isFoldingOutlineShown = false
                it.isRightMarginShown = false
                it.isShowIntentionBulb = false
                it.isUseSoftWraps = true
                it.isRefrainFromScrolling = true
                it.isAdditionalPageAtBottom = false
                it.isCaretRowShown = false
            }

            editor.addFocusListener(object : FocusChangeListener {
                override fun focusGained(focusEditor: Editor) {
                    settings.isCaretRowShown = true
                }

                override fun focusLost(focusEditor: Editor) {
                    settings.isCaretRowShown = false
                    editor.markupModel.removeAllHighlighters()
                }
            })

            return editor
        }

        fun createCodeViewer(
            project: Project,
            graphProperty: GraphProperty<String>,
            disposable: Disposable,
            language: Language,
            message: CompletableMessage,
        ): CodePartEditorInfo {
            val forceFoldEditorByDefault = message.getRole() === ChatRole.User

            val ext = CodeFence.lookupFileExt(language.displayName)
            val file = LightVirtualFile("shire.${ext}", language, graphProperty.get())

            val document: Document =
                file.findDocument() ?: throw IllegalStateException("Document not found")

            val editor: EditorEx =
                createCodeViewerEditor(project, file, document, disposable)

            val toolbarActionGroup = ActionManager.getInstance().getAction("AutoDev.ToolWindow.Snippet.Toolbar") as ActionGroup
            toolbarActionGroup.let {
                val toolbar = ActionManager.getInstance().createActionToolbar(
                    ActionPlaces.MAIN_TOOLBAR,
                    toolbarActionGroup,
                    true
                )

                toolbar.component.setBackground(editor.backgroundColor)
                toolbar.component.setOpaque(true)
                toolbar.targetComponent = editor.contentComponent
                editor.headerComponent = toolbar.component

                val connect = project.messageBus.connect(disposable)
                val topic: Topic<EditorColorsListener> = EditorColorsManager.TOPIC
                connect.subscribe(topic, EditorColorsListener {
                    toolbar.component.setBackground(editor.backgroundColor)
                })
            }

            editor.scrollPane.setBorder(JBUI.Borders.empty())
            editor.component.setBorder(JBUI.Borders.empty())

            val editorFragment = EditorFragment(editor, message)
            editorFragment.setCollapsed(forceFoldEditorByDefault)
            editorFragment.updateExpandCollapseLabel()

            return CodePartEditorInfo(graphProperty, editorFragment.getContent(), editor, file)
        }
    }
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    return ReadAction.compute<Document, Throwable> {
        FileDocumentManager.getInstance().getDocument(this)
    }
}

fun Disposable.whenDisposed(listener: () -> Unit) {
    Disposer.register(this) { listener() }
}

fun Disposable.whenDisposed(
    parentDisposable: Disposable,
    listener: () -> Unit
) {
    val isDisposed = AtomicBoolean(false)

    val disposable = Disposable {
        if (isDisposed.compareAndSet(false, true)) {
            listener()
        }
    }

    Disposer.register(this, disposable)

    Disposer.register(parentDisposable, Disposable {
        if (isDisposed.compareAndSet(false, true)) {
            Disposer.dispose(disposable)
        }
    })
}