package cc.unitmesh.devti.actions.chat.base

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

private val CANONICAL_NAME_REGEX_PATTERN = Regex("""\(([a-zA-Z]+(\.[a-zA-Z]+)+(\.[a-zA-Z0-9_]+))\)""")

/**
 * Extracts canonical names from the given item using regex.
 *
 * @param item The string containing the item.
 * @return An array of canonical names found in the item.
 */
fun getCanonicalName(input: String): List<String> {
    val matches = CANONICAL_NAME_REGEX_PATTERN.findAll(input)
    val canonicalNames = matches.map { it.value.substring(1, it.value.length - 1) }.toList()
    return canonicalNames
}

/**
 * Collects all the problems found in the given `project`, within the specified `editor` and `element`.
 *
 * @param project The project in which the problems are to be collected.
 * @param editor The editor that is associated with the element.
 * @param element The PsiElement for which the problems are to be collected.
 * @return A string containing all the problems found, separated by new lines, or `null` if no problems were found.
 */
fun collectProblems(project: Project, editor: Editor, element: PsiElement): String {
    val range = element.textRange
    val document = editor.document
    val errors: MutableList<String> = mutableListOf()
    DaemonCodeAnalyzerEx.processHighlights(document, project, null, range.startOffset, range.endOffset) {
        if (it.description != null) {
            errors.add(it.description)
        }

        true
    }

    val commentSymbol = commentPrefix(element)
    return errors.distinct().joinToString("\n") { "$commentSymbol - $it" }
}
