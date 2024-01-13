package cc.unitmesh.devti.statusbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Pair
import com.intellij.util.messages.Topic
import io.opentelemetry.api.internal.GuardedBy

class AutoDevStatusService : Disposable {
    private val lock = Any()

    @GuardedBy("lock")
    private var status = AutoDevStatus.Ready

    @GuardedBy("lock")
    private var message: String? = null


    init {
        ApplicationManager.getApplication().messageBus
            .connect(this)
            .subscribe(TOPIC, this)
    }

    override fun dispose() {

    }

    private fun getStatus(): Pair<AutoDevStatus, String?> {
        synchronized(lock) { return Pair.create(status, message) }
    }

    companion object {
        val TOPIC = Topic.create("autodev.status", AutoDevStatusService::class.java)

        val currentStatus: Pair<AutoDevStatus, String?>
            get() = ApplicationManager.getApplication().getService(AutoDevStatusService::class.java).getStatus()

    }
}