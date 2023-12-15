package cc.unitmesh.ide.webstorm.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.lang.ecmascript6.psi.ES6ExportDefaultAssignment
import com.intellij.lang.ecmascript6.psi.ES6ImportSpecifier
import com.intellij.lang.ecmascript6.psi.ES6ImportSpecifierAlias
import com.intellij.lang.ecmascript6.psi.ES6ImportedBinding
import com.intellij.lang.javascript.documentation.JSDocumentationUtils
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptModule
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.impl.JSChangeUtil
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment
import com.intellij.lang.javascript.psi.util.JSDestructuringUtil
import com.intellij.lang.javascript.psi.util.JSStubBasedPsiTreeUtil
import com.intellij.lang.javascript.psi.util.JSUtils
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
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
        val project = target.project
        val codeStyleManager = CodeStyleManager.getInstance(project)
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            val startOffset = target.textRange.startOffset
            val newEndOffset = startOffset + newDoc.length

            when (type) {
                LivingDocumentationType.COMMENT -> {
                    val existingComment = JSDocumentationUtils.findOwnDocComment(target)
                        ?: findDocFallback(target)

                    val createJSDocComment: PsiElement = JSPsiElementFactory.createJSDocComment(newDoc, target)

                    if (existingComment != null) {
                        existingComment.replace(createJSDocComment)
                    } else {
                        val parent = target.parent
                        parent.addBefore(createJSDocComment, target)
                        JSChangeUtil.addWs(parent.node, target.node, "\n")
                    }
                }

                LivingDocumentationType.ANNOTATED -> {
                    editor.document.insertString(startOffset, newDoc)
                    codeStyleManager.reformatText(target.containingFile, startOffset, newEndOffset)
                }

                LivingDocumentationType.CUSTOM -> {
                    editor.document.insertString(startOffset, newDoc)
                    codeStyleManager.reformatText(target.containingFile, startOffset, newEndOffset)
                }
            }
        })
    }

    private fun findDocFallback(documentationTarget: PsiElement): JSDocComment? {
        val parentOfDestructuring: PsiElement? by lazy {
            var context = documentationTarget.context
            while (JSDestructuringUtil.isDestructuring(context)) {
                val psiElement = context
                context = psiElement?.context
            }

            context
        }

        val filterIsInstanceFunctions = sequenceOf(
            { PsiTreeUtil.skipWhitespacesBackward(documentationTarget) },
            { parentOfDestructuring?.let { PsiTreeUtil.skipWhitespacesBackward(it) } },
            {
                parentOfDestructuring?.firstChild?.let { firstChild ->
                    if (firstChild is PsiWhiteSpace) {
                        PsiTreeUtil.skipWhitespacesForward(firstChild)
                    } else {
                        firstChild
                    }
                }
            }
        )

        val filteredSequence = filterIsInstanceFunctions.mapNotNull { it.invoke() }
        return filteredSequence.filterIsInstance<JSDocComment>().firstOrNull()
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        var candidate: PsiElement? = psiElement.parentOfTypes(PsiNameIdentifierOwner::class, JSSourceElement::class)

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
        val commonParent = CollectHighlightsUtil.findCommonParent(
            root,
            selectionModel.selectionStart,
            selectionModel.selectionEnd
        ) ?: return emptyList()

        val decls: MutableList<PsiNameIdentifierOwner> = mutableListOf()
        JSStubBasedPsiTreeUtil.processDeclarationsInScope(
            commonParent,
            { element: PsiElement, _: ResolveState ->
                if (element is PsiNameIdentifierOwner) {
                    decls.add(element)
                }
                true
            },
            true
        )

        val list = decls.filter {
            containsElement(selectionModel, it as PsiElement)
                    && isMeaningfulToDocumentInSelection(it as PsiElement)
        }.toList()

        return list.ifEmpty {
            listOfNotNull(findNearestDocumentationTarget(commonParent))
        }
    }

    private fun containsElement(selectionModel: SelectionModel, element: PsiElement): Boolean {
        return selectionModel.selectionStart <= element.textRange.startOffset && element.textRange.endOffset <= selectionModel.selectionEnd
    }

    private fun isMeaningfulToDocumentInSelection(element: PsiElement?): Boolean {
        if ((element is ES6ImportedBinding) || (element is ES6ImportSpecifierAlias) || (element is ES6ImportSpecifier)) {
            return false
        }

        if (element is JSVariable && !JSUtils.isMember(element)) {
            val initializerOrStub = element.initializerOrStub
            if (initializerOrStub is JSFunctionExpression) {
                return true
            }

            if (initializerOrStub is JSCallExpression && initializerOrStub.isRequireCall) {
                return false
            }

            val parentOfType = PsiTreeUtil.getParentOfType(element, JSSourceElement::class.java, true)
            val scope = parentOfType?.parent

            return scope is PsiFile || scope is JSEmbeddedContent || scope is TypeScriptModule
        }

        return true
    }
}