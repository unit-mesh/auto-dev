package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.IncorrectOperationException
import com.jetbrains.python.documentation.docstrings.PyDocstringGenerator
import com.jetbrains.python.psi.*


class PythonLivingDocumentation : LivingDocumentation {
    override val forbiddenRules: List<String> = listOf()

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return Pair("\"\"\"", "\"\"\"")
    }

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        if (target !is PyDocStringOwner) {
            throw IncorrectOperationException()
        }

        val docstringGenerator = PyDocstringGenerator.forDocStringOwner((target as PyDocStringOwner?)!!)
        docstringGenerator.buildAndInsert(newDoc, target)
    }

//    fun findNearestDocumentationTargetForCaret(editor: Editor): PsiNameIdentifierOwner? {
//        val element = PsiUtilBase.getElementAtCaret(editor) ?: return null
//
//        val pyFile = PsiTreeUtil.getParentOfType(element, PsiNamedElement::class.java, false) as? PyFile
//        val caretOffset = editor.caretModel.offset
//
//        return when {
//            pyFile is PyFile -> {
//                return (pyFile.topLevelClasses + pyFile.topLevelFunctions)
//                    .filterIsInstance<PsiNameIdentifierOwner>()
//                    .filter { it.textRange.startOffset >= caretOffset }
//                    .minByOrNull { it.textRange.startOffset - caretOffset }
//            }
//
//            else -> findNearestDocumentationTarget(element)
//        }
//    }

    override fun findNearestDocumentationTarget(element: PsiElement): PsiNameIdentifierOwner? {
        return when {
            element is PyFunction || element is PyClass -> element as PsiNameIdentifierOwner
            else -> {
                val closestIdentifierOwner = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
                if (closestIdentifierOwner !is PyFunction) {
                    val psiNameIdentifierOwner = PsiTreeUtil.getParentOfType(element, PyFunction::class.java) as? PsiNameIdentifierOwner
                    return psiNameIdentifierOwner ?: closestIdentifierOwner
                }
                closestIdentifierOwner
            }
        }
    }


    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        val commonParent =
            CollectHighlightsUtil.findCommonParent(root, selectionModel.selectionStart, selectionModel.selectionEnd)
                ?: return emptyList()

        return when (commonParent) {
            is PyFile -> {
                val topLevelClasses = commonParent.topLevelClasses
                val topLevelFunctions = commonParent.topLevelFunctions
                getSelectedClassesAndFunctions(topLevelClasses, topLevelFunctions, selectionModel)
            }

            is PyClass -> {
                val documentationTarget = findNearestDocumentationTarget(commonParent) ?: return emptyList()

                if (documentationTarget is PyClass && !containsElement(
                        selectionModel,
                        documentationTarget as PsiElement
                    )
                ) {
                    val nestedClasses = documentationTarget.nestedClasses.toList()
                    val methods = documentationTarget.methods.toList()
                    getSelectedClassesAndFunctions(nestedClasses, methods, selectionModel)
                } else {
                    listOf(documentationTarget)
                }
            }

            else -> emptyList()
        }
    }

    private fun getSelectedClassesAndFunctions(
        pyClasses: List<PyClass>,
        pyFunctions: List<PyFunction>,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        val filteredClasses = pyClasses.filter { intersectsElement(selectionModel, it as PsiElement) }
        val filteredFunctions = pyFunctions.filter { intersectsElement(selectionModel, it as PsiElement) }

        return filteredClasses + filteredFunctions
    }

    private fun intersectsElement(selectionModel: SelectionModel, element: PsiElement): Boolean {
        return selectionModel.selectionStart < element.textRange.endOffset && selectionModel.selectionEnd > element.textRange.startOffset
    }

    fun containsElement(selectionModel: SelectionModel, element: PsiElement): Boolean {
        return selectionModel.selectionStart <= element.textRange.startOffset && element.textRange.endOffset <= selectionModel.selectionEnd
    }

}

//private fun PyDocstringGenerator.buildAndInsert(newDoc: String) {
//    TODO("Not yet implemented")
//}

fun PyDocstringGenerator.buildAndInsert(replacementText: String, myDocStringOwner: PyDocStringOwner): PyDocStringOwner {
    val project: Project = myDocStringOwner.getProject()
    val elementGenerator = PyElementGenerator.getInstance(project)
    val replacement = elementGenerator.createDocstring(replacementText)

    val docStringExpression: PyStringLiteralExpression? = myDocStringOwner.getDocStringExpression()
    if (docStringExpression != null) {
        docStringExpression.replace(replacement.expression)
    } else {
        val container = PyUtil.`as`(
            myDocStringOwner,
            PyStatementListContainer::class.java
        ) ?: throw IncorrectOperationException("Cannot find container for docstring, Should be a function or class")

        val statements = container.statementList
        val indentation = PyIndentUtil.getElementIndent(statements)

        PyUtil.updateDocumentUnblockedAndCommitted(myDocStringOwner) { document: Document ->
            val beforeStatements = statements.prevSibling
            var replacementWithLineBreaks = """
                
                $indentation$replacementText
                """.trimIndent()
            if (statements.statements.isNotEmpty()) {
                replacementWithLineBreaks += """
                    
                    $indentation
                    """.trimIndent()
            }
            val range = beforeStatements.textRange
            if (beforeStatements !is PsiWhiteSpace) {
                document.insertString(range.endOffset, replacementWithLineBreaks)
            } else if (statements.statements.isEmpty() && beforeStatements.textContains('\n')) {
                document.insertString(range.startOffset, replacementWithLineBreaks)
            } else {
                document.replaceString(range.startOffset, range.endOffset, replacementWithLineBreaks)
            }
        }
    }
    return myDocStringOwner
}
