package cc.unitmesh.devti.context.base

import cc.unitmesh.devti.intentions.error.PsiUtils
import com.google.gson.Gson
import com.intellij.psi.PsiElement

class ElementContext(val element: PsiElement) : LLMQueryContext {
    val startLineNumber: Int
    val endLineNumber: Int

    init {
        startLineNumber = PsiUtils.getLineNumber(element, true)
        endLineNumber = PsiUtils.getLineNumber(element, false)
    }

    fun toQuery(prevLines: Int, postLines: Int, withLineNumbers: Boolean): String {
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
            string = PsiUtils.addLineNumbers(string)
        }
        return string
    }

    override fun toQuery(): String {
        return toQuery(0, 0, false)
    }

    override fun toJson(): String {
        return Gson().toJson(
            mapOf<Any, Any>(
                "startLine" to startLineNumber,
                "endLine" to endLineNumber,
                "text" to element.text
            )
        )
    }
}
