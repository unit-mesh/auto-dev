package cc.unitmesh.database.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sql.psi.SqlDefinition

class SqlLivingDocumentationProvider : LivingDocumentation {
    override val forbiddenRules: List<String>
        get() = listOf()

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return Pair("--", "--")
    }

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        val project = target.project
        val codeStyleManager = CodeStyleManager.getInstance(project)
        val file = runReadAction { target.containingFile }

        val doc = newDoc + "\n"

        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            val startOffset = target.textRange.startOffset
            val newEndOffset = startOffset + doc.length

            editor.document.insertString(startOffset, doc)
            codeStyleManager.reformatText(file, startOffset, newEndOffset)
        })
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        if (psiElement is SqlDefinition) return psiElement

        val closestId = PsiTreeUtil.getParentOfType(psiElement, PsiNameIdentifierOwner::class.java)
        if (closestId !is SqlDefinition) {
            return PsiTreeUtil.getParentOfType(psiElement, SqlDefinition::class.java) ?: closestId
        }

        return closestId
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        return emptyList()
    }
}
