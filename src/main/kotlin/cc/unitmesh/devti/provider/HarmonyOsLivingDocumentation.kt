package cc.unitmesh.devti.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.parentOfTypes

class HarmonyOsLivingDocumentation : LivingDocumentation {
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
            val text = newDoc + "\n"
            val startOffset = target.textRange.startOffset
            val newEndOffset = startOffset + text.length

            editor.document.insertString(startOffset, text)
            codeStyleManager.reformatText(target.containingFile, startOffset, newEndOffset)
        });
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        if (psiElement is PsiNameIdentifierOwner) {
            return psiElement
        }

        val candidate: PsiElement? =
            psiElement.parentOfTypes(PsiNamedElement::class, NavigatablePsiElement::class)

        if (candidate != null) {
            return candidate as? PsiNameIdentifierOwner
        }

        return null
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

        val target = findNearestDocumentationTarget(findCommonParent) ?: return emptyList()

        if (containsElement(selectionModel, target)) {
            return listOf(target)
        }

        return listOf()
    }
}