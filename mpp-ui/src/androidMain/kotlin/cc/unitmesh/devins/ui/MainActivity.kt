package cc.unitmesh.devins.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cc.unitmesh.devins.db.DatabaseDriverFactory
import cc.unitmesh.devins.ui.compose.AutoDevApp
import cc.unitmesh.devins.ui.platform.AndroidActivityProvider

/**
 * Markdown 渲染演示应用 - Android 版本
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册 Activity 以支持文件选择器功能
        AndroidActivityProvider.setActivity(this)

        // 初始化数据库
        DatabaseDriverFactory.init(this)

        enableEdgeToEdge()
        setContent {
            AutoDevApp()
        }
    }
}


