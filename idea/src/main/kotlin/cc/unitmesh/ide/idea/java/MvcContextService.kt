package cc.unitmesh.ide.idea.java

import cc.unitmesh.devti.context.DtClass
import cc.unitmesh.ide.idea.formatPsi
import cc.unitmesh.ide.idea.fromJavaFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiImportStatementBase
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope

@Service(Service.Level.PROJECT)
class MvcContextService(private val project: Project) {
    private val searchScope = GlobalSearchScope.allScope(project)
    private val javaPsiFacade = JavaPsiFacade.getInstance(project)

    private val domainRegex = Regex(".*\\.(model|entity|domain|dto)\\..*")
    private val serviceRegex = Regex(".*(service|serviceimpl)")

    private fun prepareControllerContext(controllerFile: PsiJavaFileImpl?): ControllerContext? {
        return runReadAction {
            if (controllerFile == null) return@runReadAction null

            val allImportStatements = controllerFile.importList?.allImportStatements

            return@runReadAction ControllerContext(
                services = filterImportByRegex(allImportStatements, serviceRegex),
                models = filterImportByRegex(allImportStatements, domainRegex)
            )
        }
    }

    private fun filterImportByRegex(allImportStatements: Array<out PsiImportStatementBase>?, regex: Regex) =
        allImportStatements?.filter {
            it.importReference?.text?.lowercase()?.matches(regex) ?: false
        }?.mapNotNull {
            val importText = it.importReference?.text ?: return@mapNotNull null
            javaPsiFacade.findClass(importText, searchScope)
        } ?: emptyList()

    fun servicePrompt(psiFile: PsiFile?): String {
        val file = psiFile as? PsiJavaFileImpl
        val relevantModel = prepareServiceContext(file)

        return """
${relevantModel?.joinToString("\n")}
"""
    }

    private fun prepareServiceContext(serviceFile: PsiJavaFileImpl?): List<PsiClass>? {
        return runReadAction {
            if (serviceFile == null) return@runReadAction null

            val allImportStatements = serviceFile.importList?.allImportStatements

            val entities = filterImportByRegex(allImportStatements, domainRegex)
            return@runReadAction entities
        }
    }

    fun controllerPrompt(psiFile: PsiFile?): String {
        val file = psiFile as? PsiJavaFileImpl
        val context = prepareControllerContext(file)
        val services = context?.services?.map(DtClass.Companion::formatPsi)
        val models = context?.models?.map(DtClass.Companion::formatPsi)

        val relevantModel = (services ?: emptyList()) + (models ?: emptyList())

        val clazz = fromJavaFile(file)
        return """ 
${relevantModel.joinToString("\n")}\n
// current path: ${clazz.path}
"""
    }
}
