package cc.unitmesh.devti.update

import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.time.ZonedDateTime

class AutoDevUpdateStartupActivity : ProjectActivity {
    private val RC_CHANNEL: String = "https://plugins.jetbrains.com/nightly/rc/21520"

    @RequiresBackgroundThread
    override suspend fun execute(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) return

        val now = ZonedDateTime.now()
        val lastCheck: ZonedDateTime? = AutoDevSettingsState.lastCheckTime
        if (lastCheck != null && lastCheck.plusDays(1L).isAfter(now)) {
            return
        }

        AutoDevSettingsState.lastCheckTime = now
//        CheckUpdatesTask(project).queue()
    }
}
