package cc.unitmesh.devti.statusbar

import com.intellij.util.messages.Topic

interface AutoDevStatusListener {
    fun onCopilotStatus(status: AutoDevStatus, icon: String?)

    companion object {
        val TOPIC = Topic.create("autodev.status", AutoDevStatusListener::class.java)
    }
}