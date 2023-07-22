package cc.unitmesh.devti.editor

import cc.unitmesh.devti.editor.presentation.LLMInlayRenderer
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.function.Consumer

class LLMInlayManagerImpl : LLMInlayManager {
    companion object {
        private val logger = Logger.getInstance(LLMInlayManagerImpl::class.java)

        val KEY_DOCUMENT_SAVE_VETO = Key.create<Boolean>("llm.docSaveVeto")
        private val KEY_PROCESSING =
            KeyWithDefaultValue.create("llm.processing", java.lang.Boolean.valueOf(false)) as Key<Boolean>
        private val KEY_EDITOR_SUPPORTED = Key.create<Boolean>("llm.editorSupported")
    }

    @RequiresEdt
    override fun isAvailable(editor: Editor): Boolean {
        var isAvailable: Boolean? = KEY_EDITOR_SUPPORTED[editor]
        if (isAvailable == null) {
            isAvailable = editor !is EditorWindow && editor !is ImaginaryEditor && (
                    editor !is EditorEx ||
                            !editor.isEmbeddedIntoDialogWrapper
                    ) &&
                    !editor.isViewer &&
                    !editor.isOneLineMode

            KEY_EDITOR_SUPPORTED[editor] = isAvailable
        }

        return isAvailable && !editor.isDisposed
    }


    @RequiresEdt
    override fun applyCompletion(project: Project, editor: Editor): Boolean {
        disposeInlays(editor, InlayDisposeContext.Applied)
        WriteCommandAction.runWriteCommandAction(project, "Apply Copilot Suggestion", "AutoDev", {
            if (project.isDisposed) return@runWriteCommandAction
            val document = editor.document
            try {
                KEY_DOCUMENT_SAVE_VETO[document] = true
            } finally {
                KEY_DOCUMENT_SAVE_VETO[document] = null
            }
        })

        return true
    }

    @RequiresEdt
    override fun collectInlays(editor: Editor, startOffset: Int, endOffset: Int): List<LLMInlayRenderer> {
        val model = editor.inlayModel

        val inlays = mutableListOf<Inlay<*>>().apply {
            addAll(model.getInlineElementsInRange(startOffset, endOffset))
            addAll(model.getAfterLineEndElementsInRange(startOffset, endOffset))
            addAll(model.getBlockElementsInRange(startOffset, endOffset))
        }

        return inlays.mapNotNull { it.renderer as? LLMInlayRenderer }
    }

    @RequiresEdt
    override fun disposeInlays(editor: Editor, disposeContext: InlayDisposeContext) {
        if (!isAvailable(editor) || isProcessing(editor)) {
            return
        }

        wrapProcessing(editor) { disposeInlays(collectInlays(editor, 0, editor.document.textLength)) }
    }

    override fun editorModified(editor: Editor, changeOffset: Int) {
        disposeInlays(editor, InlayDisposeContext.Typing)

        requestCompletions(editor, changeOffset) { completion ->
            if (completion.isNotEmpty()) {
                applyCompletion(editor.project!!, editor)
            }
        }
    }

    @RequiresBackgroundThread
    private fun requestCompletions(editor: Editor, changeOffset: Int, onFirstCompletion: Consumer<String>?) {
        logger.info("Requesting completions for offset $changeOffset")
    }

    override fun editorModified(editor: Editor) {
        editorModified(editor, editor.caretModel.offset)
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
