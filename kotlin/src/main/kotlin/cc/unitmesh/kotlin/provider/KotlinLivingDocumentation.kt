package cc.unitmesh.kotlin.provider

import cc.unitmesh.devti.custom.document.LivingDocumentationType
import cc.unitmesh.devti.provider.LivingDocumentation
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * The KotlinLivingDocumentation class is an implementation of the LivingDocumentation interface.
 * It provides functionality to update the documentation for a given target element based on the provided new documentation.
 * The class also includes methods to find the nearest documentation target within a given PSI element and to filter and collect
 * name identifier owners within a selection range.
 *
 * The class contains a companion object with a private logger property for logging purposes.
 *
 * The class implements the forbiddenRules property from the LivingDocumentation interface, which is a list of forbidden rules
 * for documentation. The forbidden rules include "do not return code, just documentation." and "do not use @author and @version tags."
 *
 * The class also implements the startEndString method from the LivingDocumentation interface, which returns a pair of strings
 * representing the start and end strings for different types of living documentation. The types include COMMENT, ANNOTATED, and CUSTOM.
 *
 * The class includes the updateDoc method, which updates the documentation for a given target element based on the provided new
 * documentation. The method takes in the target element, new documentation, type of living documentation, and editor as parameters.
 * The method executes as a write command action to ensure atomicity of the changes. The method handles different types of living
 * documentation and updates the documentation accordingly.
 *
 * The class also includes the containsElement method, which checks if a given selection model contains a specific PSI element.
 *
 * The class includes the findDocTargetsInSelection method, which finds the documentation targets within a given selection range.
 * The method takes in the root PSI element and selection model as parameters. It finds the common parent of the selection range
 * and filters and collects the name identifier owners within the selection range.
 *
 * The class includes the findNearestDocumentationTarget method, which finds the nearest documentation target within a given PSI element.
 * The method takes in a PSI element as a parameter and returns the nearest documentation target, which can be a named function or
 * a class or object.
 *
 * Note: This class should be used to update and find documentation for target elements in Kotlin code.
 *
 * @see LivingDocumentation
 * @see LivingDocumentationType
 */
class KotlinLivingDocumentation : LivingDocumentation {
    override val parameterTagInstruction: String get() = "use @param tag"
    override val returnTagInstruction: String get() = "use @return tag"

    companion object {
        private val logger = logger<KotlinLivingDocumentation>()
    }

    override val forbiddenRules: List<String> = listOf(
        "When given a class, do not return code, just documentation. For example, do not return `class MyClass { ... }`," +
                " just return /** ... */ comment",
        "do not use @author and @version tags."
    )

    override fun startEndString(type: LivingDocumentationType): Pair<String, String> {
        return when (type) {
            LivingDocumentationType.COMMENT -> Pair("/**", "*/")
            LivingDocumentationType.ANNOTATED -> Pair("", "")
            LivingDocumentationType.CUSTOM -> Pair("", "")
        }
    }

    /**
     * Updates the documentation for a given target element.
     *
     * @param target The target element for which the documentation needs to be updated.
     * @param newDoc The new documentation to be added.
     * @param type The type of living documentation to be updated.
     * @param editor The editor in which the target element is being edited.
     *
     * This method updates the documentation for the specified target element based on the provided new documentation.
     * The type parameter determines how the documentation is updated:
     * - If the type is LivingDocumentationType.COMMENT, the method attempts to replace the existing documentation comment
     *   with the new documentation. If the target element does not have a documentation comment, the new documentation is
     *   added before the first child of the target element.
     * - If the type is LivingDocumentationType.ANNOTATED or LivingDocumentationType.CUSTOM, the new documentation is
     *   inserted at the start offset of the target element in the editor. The code style manager is then used to reformat
     *   the text from the start offset to the new end offset.
     *
     * Note: This method is executed as a write command action to ensure that the changes are made in a single atomic
     * operation.
     *
     * Example usage:
     * ```
     * val target = // specify the target element
     * val newDoc = // specify the new documentation
     * val type = LivingDocumentationType.COMMENT // specify the type of living documentation
     * val editor = // specify the editor
     *
     * updateDoc(target, newDoc, type, editor)
     * ```
     *
     * @see LivingDocumentationType
     */
    override fun updateDoc(target: PsiElement, newDoc: String, type: LivingDocumentationType, editor: Editor) {
        val project = target.project
        val codeStyleManager = CodeStyleManager.getInstance(project)
        WriteCommandAction.runWriteCommandAction(project, "Living Document", "cc.unitmesh.livingDoc", {
            val startOffset = target.textRange.startOffset
            val newEndOffset = startOffset + newDoc.length

            when (type) {
                LivingDocumentationType.COMMENT -> {
                    try {
                        doInsertComment(target, project, newDoc)
                    } catch (e: Exception) {
                        val fromSuggestion = LivingDocumentation.buildDocFromSuggestion(newDoc, "/**", "*/")
                        if (fromSuggestion.isNotEmpty()) {
                            try {
                                doInsertComment(target, project, fromSuggestion)
                            } catch (e: Exception) {
                                logger.error("Failed to update documentation for $target, doc: $newDoc")
                            }

                            return@runWriteCommandAction
                        }

                        logger.error("Failed to update documentation for $target, doc: $newDoc")
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

    private fun doInsertComment(target: PsiElement, project: Project, newDoc: String) {
        val ktDeclaration = (if (target is KtDeclaration) target else null)
            ?: throw IncorrectOperationException()

        val createKDocFromText: PsiElement = KDocElementFactory(project).createKDocFromText(newDoc)
        val docComment = (target as KtDeclaration).docComment

        if (docComment?.replace(createKDocFromText) == null) {
            ktDeclaration.addBefore(createKDocFromText, ktDeclaration.firstChild)
        }
    }

    override fun findDocTargetsInSelection(
        root: PsiElement,
        selectionModel: SelectionModel,
    ): List<PsiNameIdentifierOwner> {
        val commonParent: PsiElement? =
            CollectHighlightsUtil.findCommonParent(root, selectionModel.selectionStart, selectionModel.selectionEnd)

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
