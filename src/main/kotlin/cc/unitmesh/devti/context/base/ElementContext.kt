package cc.unitmesh.devti.context.base

import com.intellij.temporary.AutoPsiUtils
import com.intellij.psi.PsiElement

class ElementContext(val element: PsiElement) : LLMCodeContext {
    val startLineNumber: Int
    val endLineNumber: Int

    init {
        startLineNumber = AutoPsiUtils.getLineNumber(element, true)
        endLineNumber = AutoPsiUtils.getLineNumber(element, false)
    }

    fun format(prevLines: Int, postLines: Int, withLineNumbers: Boolean): String {
        var string: String
        string = if (prevLines == 0 && postLines == 0) {
            element.text
        } else {
            val text = element.containingFile.text
            val documentLines: List<*> = text.lines()
            val fromLine = Integer.max(0, startLineNumber - prevLines)
            val toLine = Integer.min(documentLines.size, endLineNumber + postLines + 1)
            documentLines.subList(fromLine, toLine).joinToString("\n")
        }

        if (withLineNumbers) {
            string = AutoPsiUtils.addLineNumbers(string)
        }
        return string
    }

    override fun format(): String {
        return format(0, 0, false)
    }
}
