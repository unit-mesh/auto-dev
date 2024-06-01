// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.RefactorInstElement
import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.codeInsight.daemon.impl.quickfix.RenameElementFix
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

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

    private fun searchPsiElementByName(refactorInstElement: RefactorInstElement, sourceName: String): PsiNamedElement? = runReadAction {
        when {
            refactorInstElement.isMethod -> {
                val className = refactorInstElement.className
                val javaFile = this.lookupFile(sourceName) as? PsiJavaFile ?: return@runReadAction null

                val psiMethod: PsiMethod =
                    javaFile.classes.firstOrNull { it.name == className }
                        ?.methods?.firstOrNull { it.name == refactorInstElement.methodName }
                        ?: return@runReadAction null

                psiMethod
            }

            refactorInstElement.isClass -> {
                val javaFile = this.lookupFile(sourceName) as? PsiJavaFile ?: return@runReadAction null
                javaFile.classes.firstOrNull { it.name == refactorInstElement.className }
            }

            else -> {
                val javaFile = this.lookupFile(sourceName) as? PsiJavaFile ?: return@runReadAction null
                javaFile
            }
        }
    }

    /**
     * input will be canonicalName#methodName or just methodName
     */
    private fun getElementInfo(input: String, psiFile: PsiFile?): RefactorInstElement? {
        if (!input.contains("#") && psiFile != null) {
            val javaFile = psiFile as? PsiJavaFile ?: return null
            val className = javaFile.classes.firstOrNull()?.name ?: return null
            val canonicalName = javaFile.packageName + "." + className
            return RefactorInstElement(true, true, input, canonicalName, className, javaFile.packageName)
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

        return RefactorInstElement(isClass, isMethod, methodName, canonicalName, maybeClassName, pkgName)
    }
}
