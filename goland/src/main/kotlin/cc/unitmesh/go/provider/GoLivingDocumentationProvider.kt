package cc.unitmesh.go.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

class GoLivingDocumentationProvider : LivingDocumentation {
    override val forbiddenRules: List<String> get() = listOf("do not return example code")

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> = "/*" to "*/"

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        TODO("Not yet implemented")
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
