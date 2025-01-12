package cc.unitmesh.devti.inline

import com.intellij.openapi.project.Project

object AutoDevInlineChatProvider {
    fun addListener(project: Project) {
        AutoDevGutterHandler.getInstance(project).listen()
    }

    fun removeListener(project: Project) {
        AutoDevGutterHandler.getInstance(project).dispose()
    }
}