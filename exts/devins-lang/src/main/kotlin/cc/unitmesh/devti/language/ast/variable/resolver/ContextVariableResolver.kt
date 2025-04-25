package cc.unitmesh.devti.language.ast.variable.resolver

import com.intellij.lang.Language
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.CaretModel
import com.intellij.psi.PsiNameIdentifierOwner
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolver
import cc.unitmesh.devti.language.ast.variable.resolver.base.VariableResolverContext
import cc.unitmesh.devti.devins.variable.ContextVariable
import cc.unitmesh.devti.devins.variable.ContextVariable.*

class ContextVariableResolver(
    private val context: VariableResolverContext,
) : VariableResolver {
    fun all(): List<ContextVariable> = entries

    override suspend fun resolve(initVariables: Map<String, Any>): Map<String, String> = ReadAction.compute<Map<String, String>, Throwable> {
        val file = try {
            context.element?.containingFile
        } catch (e: Exception) {
            null
        }

        val caretModel = context.editor.caretModel

        all().associate { variable ->
            variable.variableName to when (variable) {
                SELECTION -> context.editor.selectionModel.selectedText
                    ?: context.editor.document.text.substring(caretModel.offset)

                SELECTION_WITH_NUM -> {
                    lineNumberedSelection(caretModel)
                }

                BEFORE_CURSOR -> file?.text?.substring(0, caretModel.offset)
                    ?: context.editor.document.text.substring(0, caretModel.offset)

                AFTER_CURSOR -> file?.text?.substring(caretModel.offset)
                    ?: context.editor.document.text.substring(caretModel.offset)

                FILE_NAME -> file?.name ?: ""
                FILE_PATH -> file?.virtualFile?.path ?: ""
                METHOD_NAME -> when (context.element) {
                    is PsiNameIdentifierOwner -> (context.element as PsiNameIdentifierOwner).nameIdentifier?.text
                        ?: ""

                    else -> ""
                }

                LANGUAGE -> context.element?.language?.displayName ?: ""
                COMMENT_SYMBOL -> getCommentSymbol(context.element?.language)

                ALL -> file?.text ?: context.editor.document.text ?: ""
            }
        }
    }

    private fun lineNumberedSelection(caretModel: CaretModel): String {
        val selection = context.editor.selectionModel.selectedText
            ?: context.editor.document.text.substring(caretModel.offset)

        var lineNo = caretModel.logicalPosition.line + 1
        return selection.split("\n").joinToString("\n") {
            val line = "$lineNo: $it"
            lineNo++
            line
        }
    }


    private fun getCommentSymbol(language: Language?): String {
        return when (language?.displayName?.lowercase()) {
            "java", "kotlin" -> "//"
            "python" -> "#"
            "javascript" -> "//"
            "typescript" -> "//"
            "go" -> "//"
            "c", "c++", "c#" -> "//"
            "rust" -> "//"
            "ruby" -> "#"
            "shell" -> "#"
            "php" -> "//"
            "perl" -> "#"
            "swift" -> "//"
            "r" -> "#"
            "scala" -> "//"
            "groovy" -> "//"
            "lua" -> "--"
            else -> "-"
        }
    }
}