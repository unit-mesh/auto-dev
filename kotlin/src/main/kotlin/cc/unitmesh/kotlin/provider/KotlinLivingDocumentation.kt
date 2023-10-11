package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.custom.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinLivingDocumentation : LivingDocumentation {
    override val docToolName: String = "Kotlin docs"
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
        val file = target.containingFile
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            val startOffset = target.textRange.startOffset
            val newEndOffset = startOffset + newDoc.length

            when (type) {
                LivingDocumentationType.COMMENT -> {
                    val ktDeclaration = (if (target is KtDeclaration) target else null)
                        ?: throw IncorrectOperationException()
                    val createKDocFromText: PsiElement = KDocElementFactory(project).createKDocFromText(newDoc)
                    val docComment = (target as KtDeclaration).docComment

                    if (docComment?.replace(createKDocFromText) == null) {
                        ktDeclaration.addBefore(createKDocFromText, ktDeclaration.firstChild)
                    }
                }

                LivingDocumentationType.ANNOTATED -> {
                    editor.document.insertString(startOffset, newDoc)
                    codeStyleManager.reformatText(file, startOffset, newEndOffset)
                }

                LivingDocumentationType.CUSTOM -> {
                    editor.document.insertString(startOffset, newDoc)
                    codeStyleManager.reformatText(file, startOffset, newEndOffset)
                }
            }
        })
    }

    private fun containsElement(selectionModel: SelectionModel, element: PsiElement): Boolean {
        return selectionModel.selectionStart <= element.textRange.startOffset && element.textRange.endOffset <= selectionModel.selectionEnd
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        val commonParent: PsiElement? =
            CollectHighlightsUtil.findCommonParent(
                root,
                selectionModel.selectionStart,
                selectionModel.selectionEnd
            )
        if (commonParent is KtFile) {
            return filterAndCollectNameIdentifierOwners(commonParent.getDeclarations(), selectionModel)
        }

        val nearestDocumentationTarget = findNearestDocumentationTarget(commonParent!!)
        if (nearestDocumentationTarget !is KtClassOrObject || containsElement(
                selectionModel,
                nearestDocumentationTarget
            )
        ) {
            return listOf(nearestDocumentationTarget!!)
        }

        val classDeclarations = nearestDocumentationTarget.getDeclarations()
        return filterAndCollectNameIdentifierOwners(classDeclarations, selectionModel)
    }

    private fun filterAndCollectNameIdentifierOwners(
        declarations: Iterable<KtDeclaration>,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        val filteredElements = declarations.filterIsInstance<PsiNameIdentifierOwner>()
            .filter { containsElement(selectionModel, it) }
        return filteredElements.toList()
    }

    override fun findNearestDocumentationTarget(psiElement: PsiElement): PsiNameIdentifierOwner? {
        if (psiElement is KtNamedFunction || psiElement is KtClassOrObject) {
            return psiElement as PsiNameIdentifierOwner
        }
        val closestIdentifierOwner = PsiTreeUtil.getParentOfType(psiElement, PsiNameIdentifierOwner::class.java)
        if (closestIdentifierOwner !is KtNamedFunction) {
            return PsiTreeUtil.getParentOfType(psiElement, KtNamedFunction::class.java) ?: closestIdentifierOwner
        }
        return closestIdentifierOwner
    }
}
