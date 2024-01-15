package cc.unitmesh.devti

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object AutoDevNotifications {
    private fun createNotificationGroup(): NotificationGroup? {
        return NotificationGroupManager.getInstance().getNotificationGroup("AutoDev.notification.group")
    }

    fun notify(project: Project, msg: String) {
        val notification = createNotificationGroup()?.createNotification(msg, NotificationType.INFORMATION)
        notification?.notify(project)
    }
}