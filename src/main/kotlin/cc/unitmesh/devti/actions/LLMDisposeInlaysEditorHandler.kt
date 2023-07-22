package cc.unitmesh.devti.actions

import cc.unitmesh.devti.editor.inaly.InlayDisposeContext
import cc.unitmesh.devti.editor.inaly.LLMInlayManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler

class LLMDisposeInlaysEditorHandler(private val baseHandler: EditorActionHandler?) : EditorActionHandler() {
    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean {
        return baseHandler != null && baseHandler.isEnabled(editor, caret, dataContext)
    }

    override fun executeInCommand(editor: Editor, dataContext: DataContext): Boolean {
        return baseHandler != null && baseHandler.executeInCommand(editor, dataContext)
    }

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        LLMInlayManager.getInstance().disposeInlays(editor, InlayDisposeContext.CaretChange)

        if (baseHandler != null && baseHandler.isEnabled(editor, caret, dataContext)) {
            baseHandler.execute(editor, caret, dataContext)
        }
    }
}
