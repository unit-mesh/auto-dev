package cc.unitmesh.rust.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement
import org.rust.lang.doc.psi.RsDocComment
import org.rust.lang.doc.psi.ext.containingDoc

class RustLivingDocumentation : LivingDocumentation {
    override val forbiddenRules: List<String> = listOf()

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return Pair("///", "///")
    }

    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        val project = target.project
        val codeStyleManager = CodeStyleManager.getInstance(project)
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            val startOffset = target.textRange.startOffset
            val newEndOffset = startOffset + newDoc.length

            when (type) {
                LivingDocumentationType.COMMENT -> {
                    val psiElementFactory = org.rust.lang.core.psi.RsPsiFactory(project)
                    val newDocComment = psiElementFactory.createBlockComment(newDoc)

                    if (target is RsDocComment) {
                        val oldDocComment = target.containingDoc
                        if (oldDocComment != null) {
                            oldDocComment.replace(newDocComment)
                        } else {
                            target.addBefore(newDocComment, target.firstChild)
                        }
                    } else {
                        target.addBefore(newDocComment, target.firstChild)
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

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        if (psiElement is RsNameIdentifierOwner) return psiElement

        val identifierOwner = PsiTreeUtil.getParentOfType(psiElement, PsiNameIdentifierOwner::class.java)
        if (identifierOwner !is RsFunction) {
            return PsiTreeUtil.getParentOfType(psiElement, RsFunction::class.java) ?: identifierOwner
        }

        return identifierOwner
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel
    ): List<PsiNameIdentifierOwner> {
        val commonParent: PsiElement? =
            CollectHighlightsUtil.findCommonParent(root, selectionModel.selectionStart, selectionModel.selectionEnd)

        if (commonParent is RsStructOrEnumItemElement) return listOf(commonParent)

        val target = findNearestDocumentationTarget(commonParent!!)
        if (target !is RsFunction || containsElement(selectionModel, target)) return listOf(target!!)

        return filterAndCollectNameIdentifierOwners(target.children, selectionModel)
    }

    private fun filterAndCollectNameIdentifierOwners(
        declarations: Array<PsiElement>,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        return declarations.filterIsInstance<PsiNameIdentifierOwner>()
            .filter { containsElement(selectionModel, it) }.toList()
    }
}
