package cc.unitmesh.go.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.goide.psi.GoFieldDeclaration
import com.goide.psi.GoMethodSpec
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.goide.psi.impl.GoElementFactory
import com.goide.psi.impl.GoPsiUtil
import com.intellij.psi.*

class GoLivingDocumentationProvider : LivingDocumentation {
    override val forbiddenRules: List<String> get() = listOf("do not return example code")

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> = "/*" to "*/"

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        val project = target.project

        val newComments = GoElementFactory.createComments(project, newDoc)
        //
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        TODO("Not yet implemented")
    }

    fun getMayBeDocumented(element: PsiElement): Boolean {
        return element is GoFieldDeclaration || element is GoMethodSpec || GoPsiUtil.isTopLevelDeclaration(element)
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        TODO("Not yet implemented")
    }

}
