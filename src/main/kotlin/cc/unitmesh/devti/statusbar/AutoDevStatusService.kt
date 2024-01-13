package cc.unitmesh.devti.statusbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

class AutoDevStatusService: Disposable {
    init {
        ApplicationManager.getApplication().messageBus
            .connect(this)
            .subscribe(TOPIC, this)
    }

    override fun dispose() {

    }

    companion object {
        val TOPIC = Topic.create("autodev.status", AutoDevStatusService::class.java)
    }
}