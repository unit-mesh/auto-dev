package cc.unitmesh.rust.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement

class RustLivingDocumentation : LivingDocumentation {
    override val forbiddenRules: List<String>
        get() = listOf()

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return Pair("///", "///")
    }

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        val project = target.project
        val file = target.containingFile
        val startOffset = target.textRange.startOffset
        val newEndOffset = startOffset + newDoc.length

        editor.document.insertString(startOffset, newDoc)
        val codeStyleManager = com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project)
        codeStyleManager.reformatText(file, startOffset, newEndOffset)
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        if (psiElement is RsNameIdentifierOwner) return psiElement

        val closestIdentifierOwner = PsiTreeUtil.getParentOfType(psiElement, PsiNameIdentifierOwner::class.java)
        if (closestIdentifierOwner !is RsFunction) {
            return PsiTreeUtil.getParentOfType(psiElement, RsFunction::class.java) ?: closestIdentifierOwner
        }

        return closestIdentifierOwner
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        val commonParent: PsiElement? =
            CollectHighlightsUtil.findCommonParent(root, selectionModel.selectionStart, selectionModel.selectionEnd)

        if (commonParent is RsStructOrEnumItemElement) {
            return listOf(commonParent)
        }

        val nearestDocumentationTarget = findNearestDocumentationTarget(commonParent!!)
        if (nearestDocumentationTarget !is RsFunction ||
            containsElement(selectionModel, nearestDocumentationTarget)
        ) {
            return listOf(nearestDocumentationTarget!!)
        }

        val classDeclarations = nearestDocumentationTarget.children
        return filterAndCollectNameIdentifierOwners(classDeclarations, selectionModel)
    }

    private fun filterAndCollectNameIdentifierOwners(
        declarations: Array<PsiElement>,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        val filteredElements = declarations.filterIsInstance<PsiNameIdentifierOwner>()
            .filter { containsElement(selectionModel, it) }
        return filteredElements.toList()
    }
}
