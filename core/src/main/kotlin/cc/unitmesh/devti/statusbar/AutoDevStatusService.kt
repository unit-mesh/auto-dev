package cc.unitmesh.devti.statusbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Pair

@Service
class AutoDevStatusService : AutoDevStatusListener, Disposable {
    private val lock = Any()

    private var status = AutoDevStatus.Ready

    private var message: String? = null

    init {
        ApplicationManager.getApplication().messageBus
            .connect(this)
            .subscribe(AutoDevStatusListener.TOPIC, this)
    }

    override fun dispose() {

    }

    private fun getStatus(): Pair<AutoDevStatus, String?> {
        synchronized(lock) { return Pair.create(status, message) }
    }

    override fun onCopilotStatus(status: AutoDevStatus, customMessage: String?) {
        synchronized(lock) {
            this.status = status
            message = customMessage
        }

        updateAllStatusBarIcons()
    }

    private fun updateAllStatusBarIcons() {
        val action = Runnable {
            ProjectManager.getInstance().openProjects
                .filterNot { it.isDisposed }
                .forEach { AutoDevStatusBarWidget.update(it) }
        }

        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            action.run()
        } else {
            application.invokeLater(action)
        }
    }

    companion object {
        val currentStatus: Pair<AutoDevStatus, String?>
            get() = ApplicationManager.getApplication().getService(AutoDevStatusService::class.java).getStatus()

        @JvmOverloads
        fun notifyApplication(status: AutoDevStatus, customMessage: String? = null) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(AutoDevStatusListener.TOPIC)
                .onCopilotStatus(status, customMessage)
        }

    }
}