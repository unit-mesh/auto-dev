package com.intellij.temporary.inlay.codecomplete

import cc.unitmesh.devti.intentions.action.task.CodeCompletionRequest
import cc.unitmesh.devti.intentions.action.task.CodeCompletionTask
import cc.unitmesh.devti.util.parser.CodeFence
import cc.unitmesh.devti.util.parser.PostCodeProcessor
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.editor.impl.InlayModelImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.intellij.temporary.inlay.codecomplete.presentation.LLMInlayRenderer
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt

class LLMInlayManagerImpl : LLMInlayManager {
    companion object {
        private val logger = logger<LLMInlayManagerImpl>()
        private val KEY_LAST_REQUEST = Key.create<CodeCompletionRequest>("llm.editorRequest")
        private val KEY_PROCESSING = KeyWithDefaultValue.create("llm.processing", false)
        private val KEY_EDITOR_SUPPORTED = Key.create<Boolean>("llm.editorSupported")
    }

    private var currentCompletion: String = ""

    @RequiresEdt
    override fun isAvailable(editor: Editor): Boolean {
        var isAvailable = KEY_EDITOR_SUPPORTED[editor]
        if (isAvailable == null) {
            isAvailable = editor !is EditorWindow && editor !is ImaginaryEditor && (
                    editor !is EditorEx || !editor.isEmbeddedIntoDialogWrapper) &&
                    !editor.isViewer && !editor.isOneLineMode

            KEY_EDITOR_SUPPORTED[editor] = isAvailable
        }

        return isAvailable && !editor.isDisposed
    }


    @RequiresEdt
    override fun applyCompletion(project: Project, editor: Editor): Boolean {
        disposeInlays(editor, InlayDisposeContext.Applied)

        val request = KEY_LAST_REQUEST[editor]
        if (request == null) {
            logger.warn("No request found for editor: $editor")
            return false
        }

        WriteCommandAction.runWriteCommandAction(project, "Apply Code Suggestion", "AutoDev", {
            if (project.isDisposed) return@runWriteCommandAction
            val document = editor.document
            try {
                document.insertString(request.offset, currentCompletion)
                editor.caretModel.moveToOffset(request.offset + currentCompletion.length)

                val range = TextRange(request.offset, request.offset + currentCompletion.length)
                val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return@runWriteCommandAction
                val codeStyleManager = CodeStyleManager.getInstance(project)
                codeStyleManager.reformatText(psiFile, range.startOffset, range.endOffset)
            } finally {
                //
            }
        })

        return true
    }


    @RequiresEdt
    override fun collectInlays(editor: Editor, startOffset: Int, endOffset: Int): List<LLMInlayRenderer> {
        val model = editor.inlayModel as? InlayModelImpl ?: return emptyList()

        if (endOffset <= startOffset) return emptyList()

        val inlays = mutableListOf<Inlay<*>>().apply {
            addAll(model.getInlineElementsInRange(startOffset, endOffset))
            addAll(model.getAfterLineEndElementsInRange(startOffset, endOffset))
            addAll(model.getBlockElementsInRange(startOffset, endOffset))
        }

        return inlays.mapNotNull { it.renderer as? LLMInlayRenderer }
    }

    @RequiresEdt
    override fun disposeInlays(editor: Editor, disposeContext: InlayDisposeContext) {
        if (!isAvailable(editor) || isProcessing(editor)) return

        wrapProcessing(editor) { disposeInlays(collectInlays(editor, 0, editor.document.textLength)) }
    }

    override fun editorModified(editor: Editor, changeOffset: Int) {
        disposeInlays(editor, InlayDisposeContext.Typing)
        requestCompletions(editor, changeOffset);
    }

    @RequiresBackgroundThread
    private fun requestCompletions(editor: Editor, changeOffset: Int) {
        val element = PsiUtilBase.getElementAtCaret(editor) ?: return
        val request = CodeCompletionRequest.create(editor, changeOffset, element, null, null) ?: return

        KEY_LAST_REQUEST[editor] = request
        CodeCompletionTask(request).execute { completion ->
            if (completion.isEmpty()) return@execute

            val completeCode = CodeFence.parse(completion).text
            currentCompletion = PostCodeProcessor(request.prefixText, request.suffixText, completeCode).execute()

            WriteCommandAction.runWriteCommandAction(editor.project) {
                val renderer = LLMInlayRenderer(editor, currentCompletion.lines())
                renderer.apply {
                    val inlay: Inlay<EditorCustomElementRenderer>? = editor.inlayModel
                        .addBlockElement(changeOffset, true, false, 0, this)
                    inlay?.let {
                        renderer.setInlay(inlay)
                    }
                }
            }
        }
    }

    override fun editorModified(editor: Editor) {
        editorModified(editor, editor.caretModel.offset)
    }

    override fun countCompletionInlays(editor: Editor, tabRange: TextRange): Int {
        val inlays = collectInlays(editor, tabRange.startOffset, tabRange.endOffset)
        if (inlays.isEmpty()) return 0

        val completionCount = inlays.count { it.getInlay()?.renderer is LLMInlayRenderer }

        if (completionCount > 0) {
            logger.debug("Completion inlays found: $completionCount")
        }

        return completionCount
    }

    private fun disposeInlays(renderers: List<LLMInlayRenderer>) {
        logger.debug("Disposing inlays: " + renderers.size)
        for (renderer in renderers) {
            val inlay = renderer.getInlay()
            if (inlay != null) {
                Disposer.dispose((inlay as Disposable?)!!)
            }
        }
    }

    private fun wrapProcessing(editor: Editor, block: Runnable) {
        assert(!(KEY_PROCESSING[editor]))
        try {
            KEY_PROCESSING[editor] = java.lang.Boolean.valueOf(true)
            block.run()
        } finally {
            KEY_PROCESSING[editor] = null
        }
    }

    private fun isProcessing(editor: Editor): Boolean {
        return KEY_PROCESSING[editor]
    }
}