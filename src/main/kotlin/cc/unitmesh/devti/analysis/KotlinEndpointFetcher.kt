package cc.unitmesh.devti.analysis

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtClass

class EndpointFetcher(val project: Project) {
    fun getAllControllerFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        // Define the search scope
        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)

        // Get all Kotlin files in the project
        val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, searchScope)

        // Filter the Kotlin files to only include those with a Controller class
        return kotlinFiles
            .map { virtualFile -> psiManager.findFile(virtualFile) }
            .filterNotNull()
            .filter { psiFile ->
                val ktClasses = PsiTreeUtil.findChildrenOfType(psiFile, KtClass::class.java)
                ktClasses.any { ktClass -> ktClass.hasAnnotation("Controller") || ktClass.hasAnnotation("RestController") }
            }
    }

    private fun KtClass.hasAnnotation(annotationName: String): Boolean {
        return this.annotationEntries.any { it.shortName.toString() == annotationName }
    }
}