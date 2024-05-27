package cc.unitmesh.devti.language.compiler.exec

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil

enum class BuiltinRefactorCommand {
    RENAME,
    SAFEDELETE,
    DELETE,
    MOVE
    ;

    companion object {
        fun fromString(command: String): BuiltinRefactorCommand? {
            return values().find { it.name.equals(command, ignoreCase = true) }
        }
    }
}

/**
 * `RefactorInsCommand` is a class that implements the `InsCommand` interface. It is responsible for executing
 * refactoring commands within a project based on the provided argument and text segment.
 *
 * The class has three private properties:
 * - `myProject`: A `Project` instance representing the current project.
 * - `argument`: A `String` containing the refactoring command to be executed.
 * - `textSegment`: A `String` containing the text segment relevant to the refactoring command.
 *
 * The `execute` method is the main entry point for executing a refactoring command. It first attempts to parse the
 * `argument` into a `BuiltinRefactorCommand` using the `fromString` method. If the command is not recognized, a
 * message indicating that it is unknown is returned.
 *
 * Depending on the type of refactoring command, the `execute` method performs different actions:
 * - For `BuiltinRefactorCommand.RENAME`: The method splits the `textSegment` using " to " and assigns the result to
 *   the variables `from` and `to`. It then performs a rename operation on a class in Java or Kotlin. The actual
 *   implementation of the rename operation is not provided in the code snippet, but it suggests using `RenameQuickFix`.
 * @see com.intellij.jvm.analysis.quickFix.RenameQuickFix for kotlin
 * @see com.intellij.spellchecker.quickfixes.RenameTo for by typos rename which will be better
 * - For `BuiltinRefactorCommand.SAFEDELETE`: The method checks the usage of the symbol before deleting it. It
 *   suggests using `SafeDeleteFix` as an example.
 * @see org.jetbrains.kotlin.idea.inspections.SafeDeleteFix for Kotlin
 * @see com.intellij.codeInsight.daemon.impl.quickfix.SafeDeleteFix for Java
 * - For `BuiltinRefactorCommand.DELETE`: The method does not perform any specific action, but it is expected to be
 *   implemented to handle the deletion of elements.
 * @see com.intellij.codeInspection.LocalQuickFixOnPsiElement
 * - For `BuiltinRefactorCommand.MOVE`: The method suggests using ` as an example for moving elements move package fix to a different package.
 * @see com.intellij.codeInspection.MoveToPackageFix
 *
 *
 * The `execute` method always returns `null`, indicating that the refactoring command has been executed, but the
 * actual result of the refactoring is not returned.
 *
 * This class is designed to be used within a refactoring tool or plugin that provides built-in refactoring commands.
 * It demonstrates how to handle different refactoring scenarios*/
class RefactorInsCommand(val myProject: Project, private val argument: String, private val textSegment: String) :
    InsCommand {
    override suspend fun execute(): String? {
        val command = BuiltinRefactorCommand.fromString(argument) ?: return "Unknown refactor command: $argument"

        when (command) {
            BuiltinRefactorCommand.RENAME -> {
                val (from, to) = textSegment.split(" to ")

                // first get the element to rename

                // in currently we only support rename class in java, kotlin
                // also use RenameQuickFix to rename element
            }

            BuiltinRefactorCommand.SAFEDELETE -> {
                // in every language, we need to check the usage of the symbol before delete it
                // SafeDeleteFix is a good example which is based on LocalQuickFixOnPsiElement

            }

            BuiltinRefactorCommand.DELETE -> {

            }

            BuiltinRefactorCommand.MOVE -> {
                // MoveToPackageFix is a good example which is based on LocalQuickFixOnPsiElement
            }
        }

        return null
    }

    fun executeRename(psiElement: PsiElement, newName: String) {
        val named = PsiTreeUtil.getNonStrictParentOfType(
            psiElement,
            PsiNamedElement::class.java
        )
        if (named == null) return
//        val name = if (named is PsiNamedElementWithCustomPresentation) named.presentationName else named.name
        val name = named.name

//        val range: TextRange = getRange(psiElement)
//        updater.rename(named, psiElement, names)
    }
}

