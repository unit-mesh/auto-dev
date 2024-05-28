// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.codeInsight.daemon.impl.quickfix.RenameElementFix
import com.intellij.codeInsight.daemon.impl.quickfix.SafeDeleteFix
import com.intellij.codeInspection.MoveToPackageFix
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.ThrowableRunnable

class JavaRefactoringTool : RefactoringTool {
    val project = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun lookupFile(path: String): PsiFile? {
        if (project == null) return null

        val elementInfo = getElementInfo(path, null) ?: return null
        val searchScope = ProjectScope.getProjectScope(project)
        val javaFiles: List<PsiJavaFile> = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? PsiJavaFile }

        val className = elementInfo.className
        val packageName = elementInfo.pkgName

        val sourceFile = javaFiles.firstOrNull {
            it.packageName == packageName && it.name == "$className.java"
        } ?: return null

        return sourceFile
    }

    override fun rename(sourceName: String, targetName: String, psiFile: PsiFile?): Boolean {
        if (project == null) return false
        val elementInfo = getElementInfo(sourceName, psiFile) ?: return false

        val element: PsiNamedElement =
            if (psiFile != null) {
                if (psiFile is PsiJavaFile) {
                    val methodName = elementInfo.methodName
                    val className = elementInfo.className

                    val psiMethod: PsiMethod? =
                        psiFile.classes.firstOrNull { it.name == className }
                            ?.methods?.firstOrNull { it.name == methodName }

                    psiMethod ?: psiFile
                } else {
                    psiFile
                }

            } else {
                searchPsiElementByName(elementInfo, sourceName) ?: return false
            }

        try {
            RenameElementFix(element, targetName)
                .invoke(project, element.containingFile, element, element)

            performRefactoringRename(project, element, targetName)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    private fun searchPsiElementByName(elementInfo: ElementInfo, sourceName: String): PsiNamedElement? = runReadAction {
        when {
            elementInfo.isMethod -> {
                val className = elementInfo.className
                val javaFile = this.lookupFile(sourceName) as? PsiJavaFile ?: return@runReadAction null

                val psiMethod: PsiMethod =
                    javaFile.classes.firstOrNull { it.name == className }
                        ?.methods?.firstOrNull { it.name == elementInfo.methodName }
                        ?: return@runReadAction null

                psiMethod
            }

            elementInfo.isClass -> {
                val javaFile = this.lookupFile(sourceName) as? PsiJavaFile ?: return@runReadAction null
                javaFile.classes.firstOrNull { it.name == elementInfo.className }
            }

            else -> {
                val javaFile = this.lookupFile(sourceName) as? PsiJavaFile ?: return@runReadAction null
                javaFile
            }
        }
    }

    data class ElementInfo(
        val isClass: Boolean,
        val isMethod: Boolean,
        val methodName: String,
        val canonicalName: String,
        val className: String,
        val pkgName: String
    )

    /**
     * input will be canonicalName#methodName or just methodName
     */
    private fun getElementInfo(input: String, psiFile: PsiFile?): ElementInfo? {
        if (!input.contains("#") && psiFile != null) {
            val javaFile = psiFile as? PsiJavaFile ?: return null
            val className = javaFile.classes.firstOrNull()?.name ?: return null
            val canonicalName = javaFile.packageName + "." + className
            return ElementInfo(true, true, input, canonicalName, className, javaFile.packageName)
        }

        val isMethod = input.contains("#")
        val methodName = input.substringAfter("#")
        val canonicalName = input.substringBefore("#")
        val maybeClassName = canonicalName.substringAfterLast(".")
        // the clasName should be Uppercase or it will be the package
        var isClass = false
        var pkgName = canonicalName.substringBeforeLast(".")
        if (maybeClassName[0].isLowerCase()) {
            pkgName = "$pkgName.$maybeClassName"
        } else {
            isClass = true
        }

        return ElementInfo(isClass, isMethod, methodName, canonicalName, maybeClassName, pkgName)
    }

    override fun safeDelete(element: PsiElement): Boolean {
        val delete = SafeDeleteFix(element)
        try {
            delete.invoke(element.project, element.containingFile, element, element)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    /**
     * This method is used to move a given element to a specified target.
     *
     * @param element The PsiNameIdentifierOwner object that is to be moved. This object represents the element in
     * the PSI (Program Structure Interface) tree.
     * @param target The target location where the element is to be moved. This is a string representing the canonical
     * name of the target.
     *
     * @return A boolean value indicating the success of the operation. Returns true if the element is
     * successfully moved to the target location, false otherwise.
     */
    override fun move(element: PsiElement, canonicalName: String): Boolean {
        val file = element.containingFile
        val fix = MoveToPackageFix(file, canonicalName)

        try {
            fix.invoke(file.project, file, element, element)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    protected fun isRenamerFactoryApplicable(
        renamerFactory: AutomaticRenamerFactory,
        elementToRename: PsiNamedElement
    ): Boolean {
        return renamerFactory.isApplicable(elementToRename)
    }

    /// the AutomaticRenamerFactory support for testing rename
    fun performRefactoringRename(myProject: Project, elementToRename: PsiNamedElement, newName: String) {
        for (renamerFactory in AutomaticRenamerFactory.EP_NAME.extensionList) {
            if (!isRenamerFactoryApplicable(renamerFactory, elementToRename)) continue
            val usages: List<UsageInfo> = ArrayList()
            val renamer = renamerFactory.createRenamer(elementToRename, newName, ArrayList())
            if (!renamer.hasAnythingToRename()) continue

            val runnable = Runnable {
                ApplicationManager.getApplication().runReadAction {
                    renamer.findUsages(usages, false, false)
                }
            }

            if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    runnable, RefactoringBundle.message("searching.for.variables"), true, myProject
                )
            ) {
                return
            }

            if (!CommonRefactoringUtil.checkReadOnlyStatus(
                    myProject,
                    *PsiUtilCore.toPsiElementArray(renamer.elements)
                )
            ) return

            val performAutomaticRename = ThrowableRunnable<RuntimeException> {
                CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject)
                val classified = RenameProcessor.classifyUsages(renamer.elements, usages)
                for (element in renamer.elements) {
                    val newElementName = renamer.getNewName(element)
                    if (newElementName != null) {
                        val infos = classified[element]
                        RenameUtil.doRename(
                            element,
                            newElementName,
                            infos.toTypedArray(),
                            myProject,
                            RefactoringElementListener.DEAF
                        )
                    }
                }
            }

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.writeCommandAction(myProject)
                    .withName(getCommandName()).run(performAutomaticRename)
            }
        }
    }

    private fun getCommandName(): @NlsContexts.Command String? {
        return "Rename"
    }
}
