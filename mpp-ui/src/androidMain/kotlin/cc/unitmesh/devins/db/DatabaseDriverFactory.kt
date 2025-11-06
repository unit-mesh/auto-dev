package cc.unitmesh.devins.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android 平台的数据库驱动工厂
 * 需要在应用启动时调用 DatabaseDriverFactory.init(context) 初始化
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val context = applicationContext
            ?: throw IllegalStateException("DatabaseDriverFactory not initialized. Call DatabaseDriverFactory.init(context) first.")

        return AndroidSqliteDriver(
            schema = DevInsDatabase.Schema,
            context = context,
            name = "autodev.db"
        )
    }

    companion object {
        private var applicationContext: Context? = null

        /**
         * 初始化数据库工厂（在 Application 或 Activity 中调用）
         */
        fun init(context: Context) {
            applicationContext = context.applicationContext
        }
    }
}
