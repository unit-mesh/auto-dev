package cc.unitmesh.devins.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cc.unitmesh.devins.db.DatabaseDriverFactory
import cc.unitmesh.devins.ui.compose.AutoDevApp
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.platform.AndroidActivityProvider

/**
 * AutoDev 移动应用 - Android 版本
 * 支持主题切换
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册 Activity 以支持文件选择器功能
        AndroidActivityProvider.setActivity(this)

        // 初始化数据库
        DatabaseDriverFactory.init(this)

        // 初始化配置管理器（必须在使用前调用）
        ConfigManager.initialize(this)

        enableEdgeToEdge()
        setContent {
            // AutoDevApp 内部已经包含 AutoDevTheme
            AutoDevApp()
        }
    }
}
