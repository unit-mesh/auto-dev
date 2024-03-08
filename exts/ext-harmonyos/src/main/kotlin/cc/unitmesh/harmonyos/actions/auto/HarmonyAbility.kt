package cc.unitmesh.harmonyos.actions.auto

enum class HarmonyAbility(
    val abilityName: String,
    val description: String,
    val sample: String,
) {
    Notification(
        "Notification",
        "基础类型通知主要应用于发送短信息、提示信息、广告推送等，支持普通文本类型、长文本类型、多行文本类型和图片类型。",
        """
        let notificationRequest = {
          id: 1,
          content: {
            contentType: NotificationManager.ContentType.NOTIFICATION_CONTENT_LONG_TEXT, // 长文本类型通知
            longText: {
              title: 'test_title',
              text: 'test_text',
              additionalText: 'test_additionalText',
              longText: 'test_longText',
              briefText: 'test_briefText',
              expandedTitle: 'test_expandedTitle',
            }
          }
        }
        
        // 发布通知
        NotificationManager.publish(notificationRequest, (err) => {
            if (err) {
                console.error(`[ANS] failed to publish, error[${'$'}{err}]`);
                return;
            }
            console.info(`[ANS] publish success`);
        });
        """.trimIndent()
    ),
    WindowManage(
        "窗口管理",
        "窗口模块用于在同一块物理屏幕上，提供多个应用界面显示、交互的机制。",
        """"""
    )
}