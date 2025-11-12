package cc.unitmesh.devins.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cc.unitmesh.devins.db.DatabaseDriverFactory
import cc.unitmesh.devins.ui.app.SessionApp
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.platform.AndroidActivityProvider

/**
 * AutoDev 移动应用 - Android 版本
 * 支持会话管理、项目管理、任务管理和 AI Agent 执行
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidActivityProvider.setActivity(this)

        // 初始化数据库
        DatabaseDriverFactory.init(this)

        // 初始化配置管理器（必须在使用前调用）
        ConfigManager.initialize(this)

        enableEdgeToEdge()
        setContent {
            // 使用 SessionApp，启用底部导航
            SessionApp(
                serverUrl = "http://10.0.2.2:8080", // Android 模拟器访问本机
                useBottomNavigation = true
            )
        }
    }
}
