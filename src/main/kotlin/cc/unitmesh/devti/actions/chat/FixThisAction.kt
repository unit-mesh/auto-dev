package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.provider.PsiElementDataBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.util.regex.Matcher

class FixThisAction : RefactorThisAction() {
    init {
        getTemplatePresentation().text = AutoDevBundle.message("settings.autodev.rightClick.fixthis")
    }

    override fun getActionType(): ChatActionType = ChatActionType.FIX_ISSUE

    override fun addAdditionPrompt(project: Project, editor: Editor, element: PsiElement): String {
        val commentSymbol = commentPrefix(element)

        return collectProblems(project, editor, element)?.let { problem ->
            var relatedCode = ""
            getCanonicalName(problem).map {
                val classContext = PsiElementDataBuilder.forLanguage(element.language)?.lookupElement(project, it)
                classContext.let { context ->
                    relatedCode += context?.format() ?: ""
                }
            }

            buildString {
                append("\n\n$commentSymbol relative static analysis result:\n$problem")
                if (relatedCode.isNotEmpty()) {
                    relatedCode.split("\n").forEach {
                        append("\n$commentSymbol $it")
                    }
                }
            }
        } ?: ""
    }

    companion object {
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
    }
}
