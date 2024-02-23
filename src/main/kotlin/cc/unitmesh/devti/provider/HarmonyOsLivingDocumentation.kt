package cc.unitmesh.devti.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.parentOfTypes

class HarmonyOsLivingDocumentation : LivingDocumentation {
    override val forbiddenRules: List<String> = listOf(
        "ArkTS is an extension of TypeScript, you can use TypeScript's rules",
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

            editor.document.insertString(startOffset, newDoc)
            codeStyleManager.reformatText(target.containingFile, startOffset, newEndOffset)
        });
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        if (psiElement is PsiNameIdentifierOwner) {
            return psiElement
        }

        var candidate: PsiElement? =
            psiElement.parentOfTypes(PsiNameIdentifierOwner::class, NavigatablePsiElement::class)

        while (candidate != null) {
            if (candidate is PsiNameIdentifierOwner) {
                return candidate
            }

            candidate = candidate.parentOfTypes(PsiNameIdentifierOwner::class, NavigatablePsiElement::class)
        }

        return null
    }

    override fun findDocTargetsInSelection(
        psiElement: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        return listOf()
    }
}