package cc.unitmesh.pycharm.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.IncorrectOperationException
import com.jetbrains.python.documentation.docstrings.PyDocstringGenerator
import com.jetbrains.python.psi.PyDocStringOwner

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
//        docstringGenerator.buildAndInsert(newDoc)
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        TODO("Not yet implemented")
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        TODO("Not yet implemented")
    }

}
