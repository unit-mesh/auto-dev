package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.codeInsight.daemon.impl.quickfix.SafeDeleteFix
import com.intellij.codeInspection.MoveToPackageFix
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.jvm.analysis.quickFix.RenameQuickFix
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.ProjectScope

class JavaRefactoringTool : RefactoringTool {
    val project = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun lookupFile(path: String): PsiFile? {
        if (project == null) return null

        val elementInfo = getElementInfo(path)
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

    override fun rename(sourceName: String, targetName: String): Boolean {
        if (project == null) {
            return false
        }

        val elementInfo = getElementInfo(sourceName)
        val psiFile: PsiFile? = null

        val element = runReadAction {
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
        } ?: return false

        if (psiFile == null) return false

        val renameQuickFix = RenameQuickFix(element, targetName)
        val startElement = element
        val endElement = element

        try {
            renameQuickFix.invoke(project, psiFile, startElement, endElement)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    data class ElementInfo(
        val isClass: Boolean,
        val isMethod: Boolean,
        val methodName: String,
        val canonicalName: String,
        val className: String,
        val pkgName: String
    )

    private fun getElementInfo(input: String): ElementInfo {
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
}
