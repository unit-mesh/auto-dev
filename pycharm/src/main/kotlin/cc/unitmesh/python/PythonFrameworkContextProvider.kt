package cc.unitmesh.python

import cc.unitmesh.devti.provider.context.ChatContextItem
import cc.unitmesh.devti.provider.context.ChatContextProvider
import cc.unitmesh.devti.provider.context.ChatCreationContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.mostPreferred

class PythonFrameworkContextProvider : ChatContextProvider {
    override fun isApplicable(project: Project, creationContext: ChatCreationContext): Boolean {
        return creationContext.element?.language is PythonLanguage
    }

    override fun collect(project: Project, creationContext: ChatCreationContext): List<ChatContextItem> {
        var items = mutableListOf<ChatContextItem>()
        val allSdks = PythonSdkUtil.getAllSdks()
        val preferred = mostPreferred(allSdks)

        val myInterpreterList = PyConfigurableInterpreterList.getInstance(project)
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk
            ?: preferred
            ?: myInterpreterList.allPythonSdks.firstOrNull()

        if (projectSdk != null) {
            val context = "This project is using Python SDK ${projectSdk.name}"
            items.add(ChatContextItem(PythonFrameworkContextProvider::class, context))
        }

        return items
    }
}