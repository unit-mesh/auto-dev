package cc.unitmesh.devti.prompting

import cc.unitmesh.devti.analysis.DtClass
import cc.unitmesh.devti.analysis.DtClass.Companion.fromPsiClass
import cc.unitmesh.devti.prompting.model.ControllerContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope

class MvcContextFactory(
    val project: Project,
    val javaPsiFacade: JavaPsiFacade,
    val globalSearchScope: GlobalSearchScope
) {
    fun prepareControllerContext(controllerFile: PsiJavaFileImpl?): ControllerContext? {
        return runReadAction {
            if (controllerFile == null) return@runReadAction null

            val allImportStatements = controllerFile.importList?.allImportStatements

            val services = allImportStatements?.filter {
                it.importReference?.text?.endsWith("Service", true) ?: false
            }?.mapNotNull {
                val importText = it.importReference?.text ?: return@mapNotNull null
                javaPsiFacade.findClass(importText, globalSearchScope)
            } ?: emptyList()

            val entities = allImportStatements?.filter {
                it.importReference?.text?.matches(Regex(".*\\.(model|entity|domain|dto)\\..*")) ?: false
            }?.mapNotNull {
                val importText = it.importReference?.text ?: return@mapNotNull null
                javaPsiFacade.findClass(importText, globalSearchScope)
            } ?: emptyList()

            return@runReadAction ControllerContext(
                services = services,
                models = entities
            )
        }
    }

    fun createServicePrompt(psiFile: PsiFile?): String {
        val file = psiFile as? PsiJavaFileImpl
        val relevantModel = prepareServiceContext(file)

        return """Complete java code, return rest code, no explaining.
${relevantModel?.joinToString("\n")}
"""
    }

    fun prepareServiceContext(serviceFile: PsiJavaFileImpl?): List<PsiClass>? {
        return runReadAction {
            if (serviceFile == null) return@runReadAction null

            val allImportStatements = serviceFile.importList?.allImportStatements

            val entities = allImportStatements?.filter {
                it.importReference?.text?.matches(Regex(".*\\.(model|entity|domain)\\..*")) ?: false
            }?.mapNotNull {
                val importText = it.importReference?.text ?: return@mapNotNull null
                javaPsiFacade.findClass(importText, globalSearchScope)
            } ?: emptyList()

            return@runReadAction entities
        }
    }

    fun createControllerPrompt(psiFile: PsiFile?): String {
        val file = psiFile as? PsiJavaFileImpl
        val context = prepareControllerContext(file)
        val services = context?.services?.map {
            DtClass.fromPsiClass(it).format()
        }
        val models = context?.models?.map {
            DtClass.fromPsiClass(it).format()
        }

        val relevantModel = (services ?: emptyList()) + (models ?: emptyList())

        val clazz = DtClass.fromJavaFile(file)
        return """Complete java code, return rest code, no explaining. 
```java
${relevantModel.joinToString("\n")}\n
// current path: ${clazz.path}
"""
    }
}
