package cc.unitmesh.devti.update

import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.time.ZonedDateTime

//class AutoDevUpdateStartupActivity : ProjectActivity {
//    private val RC_CHANNEL: String = "https://plugins.jetbrains.com/plugins/rc/21520"
//
//    @RequiresBackgroundThread
//    override suspend fun execute(project: Project) {
//        if (ApplicationManager.getApplication().isUnitTestMode) return
//
////        val pluginHosts = UpdateSettings.getInstance().storedPluginHosts
////        if (!pluginHosts.contains(RC_CHANNEL)) {
////            pluginHosts.add(RC_CHANNEL)
////        }
//
//        val now = ZonedDateTime.now()
//        val lastCheck: ZonedDateTime? = AutoDevSettingsState.lastCheckTime
//        if (lastCheck != null && lastCheck.plusDays(1L).isAfter(now)) {
//            return
//        }
//
//        AutoDevSettingsState.lastCheckTime = now
//        CheckUpdatesTask(project).queue()
//    }
//}

class CheckUpdatesTask @JvmOverloads constructor(project: Project, private val notifyNoUpdate: Boolean = false) :
    Task.Backgroundable(project, "Check Update", true) {
    override fun run(indicator: ProgressIndicator) {
        // chore: add notification for no update

//        StartupManager.getInstance(project).runWhenProjectIsInitialized {
//            AutoDevNotifications.notify(project, "AutoDev Update Available")
//        }
    }
}
