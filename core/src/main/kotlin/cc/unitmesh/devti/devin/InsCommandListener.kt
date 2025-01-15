package cc.unitmesh.devti.devin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic

/**
 * Provide for listening to the status of InsCommand
 */
interface InsCommandListener {
    fun onFinish(command: InsCommand, status: InsCommandStatus, file: VirtualFile?)

    companion object {
        val TOPIC = Topic.create("autodev.inscommand.status", InsCommandListener::class.java)

        fun notify(command: InsCommand, status: InsCommandStatus, file: VirtualFile?) {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(TOPIC)
                .onFinish(command, status, file)
        }
    }
}