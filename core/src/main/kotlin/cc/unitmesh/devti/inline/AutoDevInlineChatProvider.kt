package cc.unitmesh.devti.inline

import com.intellij.openapi.project.Project

object AutoDevInlineChatProvider {
    var isAdded = false

    fun addListener(project: Project) {
        if (isAdded) return
        AutoDevGutterHandler.getInstance(project).listen()
    }

    fun removeListener(project: Project) {
        AutoDevGutterHandler.getInstance(project).dispose()
    }
}