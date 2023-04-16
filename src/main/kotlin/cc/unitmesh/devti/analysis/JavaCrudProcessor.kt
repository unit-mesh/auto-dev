package cc.unitmesh.devti.analysis

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil


class JavaCrudProcessor(val project: Project) : CrudProcessor {
    private val psiElementFactory = JavaPsiFacade.getElementFactory(project)
    private val controllers = getAllControllerFiles()

    private fun getAllControllerFiles(): List<PsiFile> {
        val psiManager = PsiManager.getInstance(project)

        val searchScope: GlobalSearchScope = ProjectScope.getContentScope(project)
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, searchScope)

        return filterFiles(javaFiles, psiManager, ::controllerFilter)
    }

    private fun filterFiles(
        javaFiles: Collection<VirtualFile>,
        psiManager: PsiManager,
        filter: (PsiClass) -> Boolean
    ) = javaFiles
        .mapNotNull { virtualFile -> psiManager.findFile(virtualFile) }
        .filter { psiFile ->
            val psiClass = PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java)
                .firstOrNull()
            psiClass != null && filter(psiClass)
        }

    private fun controllerFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Controller" ||
                    it == "org.springframework.web.bind.annotation.RestController"
        }

    private fun serviceFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Service"
        }

    private fun repositoryFilter(clazz: PsiClass): Boolean = clazz.annotations
        .map { it.qualifiedName }.any {
            it == "org.springframework.stereotype.Repository"
        }

    fun addMethodToClass(psiClass: PsiClass, method: String): PsiClass {
        val methodFromText = psiElementFactory.createMethodFromText(method, psiClass)
        var lastMethod: PsiMethod? = null
        val allMethods = psiClass.methods

        if (allMethods.isNotEmpty()) {
            lastMethod = allMethods[allMethods.size - 1]
        }

        if (lastMethod != null) {
            psiClass.addAfter(methodFromText, lastMethod)
        } else {
            psiClass.add(methodFromText)
        }

        return psiClass
    }

    override fun controllerList(): List<DtClass> {
        return this.controllers.map {
            val className = it.name.substring(0, it.name.length - ".java".length)
            DtClass.fromPsiFile(it) ?: DtClass(className, emptyList())
        }
    }

    override fun serviceList(): List<DtClass> {
        TODO("Not yet implemented")
    }

    override fun modelList(): List<DtClass> {
        TODO("Not yet implemented")
    }

    override fun updateMethod(targetController: String, code: String) {
        val targetControllerFile = controllers.first { it.name == "$targetController.java" }
        val targetControllerClass = PsiTreeUtil.findChildrenOfType(targetControllerFile, PsiClass::class.java)
            .firstOrNull() ?: return

        val updatedClass = addMethodToClass(targetControllerClass, code)
        val updatedFile = updatedClass.containingFile
        updatedFile.add(updatedClass)
    }
}
