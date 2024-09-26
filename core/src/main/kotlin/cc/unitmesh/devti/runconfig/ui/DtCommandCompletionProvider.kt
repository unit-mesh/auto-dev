package cc.unitmesh.devti.runconfig.ui

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil

class DtCommandCompletionProvider : TextFieldCompletionProvider() {
    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
        val element = LookupElementBuilder.create("devti auto").withInsertHandler { ctx, _ ->
            ctx.addSuffix(" ")
        }

        result.addElement(element)
    }
}

fun InsertionContext.addSuffix(suffix: String) {
    document.insertString(selectionEndOffset, suffix)
    EditorModificationUtil.moveCaretRelatively(editor, suffix.length)
}
