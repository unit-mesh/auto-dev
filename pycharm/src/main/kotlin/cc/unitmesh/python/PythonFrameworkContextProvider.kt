package cc.unitmesh.python

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.python.PythonLanguage

class PythonFrameworkContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        println(creationContext.element?.language)
        return creationContext.element?.language is PythonLanguage
    }

    @RequiresBackgroundThread
    override suspend fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
//        val packages: List<PyPackage> = project.modules.asSequence()
//            .mapNotNull {
//                val pair = PyProjectSdkConfigurationExtension.findForModule(it)
//                pair?.second?.createAndAddSdkForInspection(it)
//            }
//            .mapNotNull {
//                tryCreateCustomPackageManager(it)?.packages
//            }
//            .flatten().toList()
//
//        /// check has Django, Flask or FastAPI and return the context
//        when {
//            packages.any { it.name == "django" } -> {
//                return listOf(ChatContextItem(PythonFrameworkContextProvider::class, "This project uses Django"))
//            }
//            packages.any { it.name == "flask" } -> {
//                return listOf(ChatContextItem(PythonFrameworkContextProvider::class, "This project uses Flask"))
//            }
//            packages.any { it.name == "fastapi" } -> {
//                return listOf(ChatContextItem(PythonFrameworkContextProvider::class, "This project uses FastAPI"))
//            }
//        }

        return listOf()
    }
}