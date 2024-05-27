package cc.unitmesh.idea.provider

import cc.unitmesh.devti.provider.RefactoringTool
import com.intellij.codeInsight.daemon.impl.quickfix.SafeDeleteFix
import com.intellij.codeInspection.MoveToPackageFix
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.jvm.analysis.quickFix.RenameQuickFix
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope


class JavaRefactoringTool : RefactoringTool {
    val project = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun rename(sourceName: String, targetName: String): Boolean {
        if (project == null) {
            return false
        }

        val searchScope = GlobalSearchScope.allScope(project)
        val elementInfo = getElementInfo(targetName)
        var psiFile: com.intellij.psi.PsiFile? = null

        // find psi element by cannocial name which is sourceName
        val element = runReadAction {
            if (elementInfo.isMethod) {
                val javaFiles: Collection<VirtualFile> = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)
                val className = elementInfo.className
                val packageName = elementInfo.pkgName

                val sourceFile: VirtualFile? = javaFiles.firstOrNull {
                    it.name == "$className.java" && it.parent.name == packageName
                }

                if (sourceFile == null) {
                    return@runReadAction null
                }

                psiFile = PsiManager.getInstance(project).findFile(sourceFile) ?: return@runReadAction null
                val javaFile = psiFile as? com.intellij.psi.PsiJavaFile ?: return@runReadAction null

                val psiMethod: PsiMethod =
                    javaFile.classes.firstOrNull { it.name == className }?.methods?.firstOrNull { it.name == elementInfo.methodName }
                        ?: return@runReadAction null

                psiMethod
            } else {
                val sourceFile = LocalFileSystem.getInstance().findFileByPath(sourceName) ?: return@runReadAction null
                PsiManager.getInstance(project).findFile(sourceFile)
            }
        } ?: return false

        val renameQuickFix = RenameQuickFix(element, targetName)
        val startElement = element
        val endElement = element

        try {
            renameQuickFix.invoke(project, psiFile!!, startElement, endElement)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    data class ElementInfo(
        val isMethod: Boolean,
        val methodName: String,
        val canonicalName: String,
        val className: String,
        val pkgName: String
    )

    private fun getElementInfo(targetName: String): ElementInfo {
        val isMethod = targetName.contains("#")
        val methodName = targetName.substringAfter("#")
        val canonicalName = targetName.substringBefore("#")
        val className = canonicalName.substringAfterLast(".")
        // the clasName should be Uppercase or it will be the package
        var pkgName = canonicalName.substringBeforeLast(".")
        if (className[0].isLowerCase()) {
            pkgName = "$pkgName.$className"
        }

        return ElementInfo(isMethod, methodName, canonicalName, className, pkgName)
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
