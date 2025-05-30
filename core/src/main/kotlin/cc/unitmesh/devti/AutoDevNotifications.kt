package cc.unitmesh.devti

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object AutoDevNotifications {
    private fun group(): NotificationGroup? {
        return NotificationGroupManager.getInstance().getNotificationGroup("AutoDev.notification.group")
    }

    fun notify(project: Project, msg: String, type: NotificationType? = NotificationType.INFORMATION) {
        if (type == null) {
            info(project, msg)
        } else {
            group()?.createNotification(msg, type)?.notify(project)
        }
    }

    fun error(project: Project, msg: String) {
        group()?.createNotification(msg, NotificationType.ERROR)?.notify(project)
    }

    fun warn(project: Project, msg: String) {
        group()?.createNotification(msg, NotificationType.WARNING)?.notify(project)
    }

    fun info(project: Project, msg: String) {
        group()?.createNotification(msg, NotificationType.INFORMATION)?.notify(project)
    }
}
