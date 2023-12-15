package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptObjectType
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.util.JSUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfTypes

class JavaScriptLivingDocumentation : LivingDocumentation {
    override val forbiddenRules: List<String> = listOf(
        "do not return example code",
        "do not use @author and @version tags"
    )

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return when (type) {
            LivingDocumentationType.COMMENT -> Pair("/**", "*/")
            LivingDocumentationType.ANNOTATED -> Pair("", "")
            LivingDocumentationType.CUSTOM -> Pair("", "")
        }
    }

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        TODO("Not yet implemented")
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        var candidate: PsiElement? = null

        // lookup
        if (psiElement is PsiWhiteSpace) {
            val parent = psiElement.parent
            when (parent) {
                is JSFile, is JSEmbeddedContent, is JSObjectLiteralExpression, is JSBlockStatement, is TypeScriptObjectType -> {
                    candidate = PsiTreeUtil.skipWhitespacesAndCommentsForward(psiElement)
                }
                is JSClass -> {
                    val next = PsiTreeUtil.skipWhitespacesAndCommentsForward(psiElement)
                    if (JSUtils.isMember(next)) {
                        candidate = next
                    }
                }
            }
        }

        // find to parent
        if (candidate == null) {
            candidate = psiElement.parentOfTypes(PsiNameIdentifierOwner::class, JSSourceElement::class)
        }

        if (candidate is JSParameter) {
            candidate = candidate.declaringFunction
        }

        // by element type
        when (psiElement) {
            is JSExpressionStatement -> {
                val expression: JSAssignmentExpression? = psiElement.expression as? JSAssignmentExpression
                if (expression is JSAssignmentExpression) {
                    val lOperand = expression.lOperand
                    if (lOperand is JSDefinitionExpression) {
                        candidate = lOperand
                    }
                }
            }
            is ES6ExportDefaultAssignment -> {
                candidate = psiElement.expression
            }
            is JSProperty, is JSFunction, is JSVariable, is JSClass, is JSField -> {
                candidate = psiElement
            }
            is JSVarStatement -> {
                val variables = psiElement.variables
                if (variables.isNotEmpty()) {
                    candidate = variables[0] as PsiElement
                }
            }
        }

        return candidate as? PsiNameIdentifierOwner
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        val findCommonParent = CollectHighlightsUtil.findCommonParent(
            root,
            selectionModel.selectionStart,
            selectionModel.selectionEnd
        ) ?: return emptyList()

        return emptyList()
    }

    private fun containsElement(selectionModel: SelectionModel, element: PsiElement): Boolean {
        return selectionModel.selectionStart <= element.textRange.startOffset && element.textRange.endOffset <= selectionModel.selectionEnd
    }
}