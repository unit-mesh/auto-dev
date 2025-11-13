package cc.unitmesh.devins.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cc.unitmesh.devins.db.DatabaseDriverFactory
import cc.unitmesh.devins.ui.compose.AutoDevApp
import cc.unitmesh.devins.ui.compose.PlatformAutoDevApp
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.devins.ui.platform.AndroidActivityProvider

/**
 * AutoDev 移动应用 - Android 版本
 *
 * 默认使用本地模式，支持本地和远程两种 Agent 模式
 * 用户可以在应用内通过 UI 切换模式，配置会保存到 ~/.autodev/config.yaml
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
            // 使用 AutoDevApp，支持本地和远程模式切换
            PlatformAutoDevApp()
        }
    }
}
