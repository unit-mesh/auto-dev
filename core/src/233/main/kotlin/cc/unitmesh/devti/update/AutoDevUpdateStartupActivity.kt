package cc.unitmesh.devti.update

import cc.unitmesh.devti.inline.AutoDevInlineChatProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

class AutoDevUpdateStartupActivity : ProjectActivity {
    @RequiresBackgroundThread
    override suspend fun execute(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) return

        AutoDevInlineChatProvider.addListener(project)
    }
}