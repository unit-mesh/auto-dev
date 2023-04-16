package cc.unitmesh.devti.analysis

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil

class JavaEndpointFetcher(val project: Project) {
    fun getAllControllerFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)

        return javaFiles
            .mapNotNull { virtualFile -> psiManager.findFile(virtualFile) }
            .filter { psiFile ->
                val javaClasses = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
                javaClasses.any { clazz -> clazz.hasAnnotation("Controller") || clazz.hasAnnotation("RestController") }
            }
    }
}