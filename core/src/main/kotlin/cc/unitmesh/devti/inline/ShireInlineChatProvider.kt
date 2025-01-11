package cc.unitmesh.devti.inline

import com.intellij.openapi.project.Project
import com.phodal.shire.inline.AutoDevGutterHandler

object ShireInlineChatProvider {
    fun addListener(project: Project) {
        AutoDevGutterHandler.getInstance(project).listen()
    }

    fun removeListener(project: Project) {
        AutoDevGutterHandler.getInstance(project).dispose()
    }
}