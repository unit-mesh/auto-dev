package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val homeDir = System.getProperty("user.home")

        // 检测是否在 Android 环境中（user.home 为 "/" 表示 Android）
        val dbDir = if (homeDir == "/" || homeDir.isNullOrEmpty()) {
            // Android 环境：使用临时目录
            val tmpDir = System.getProperty("java.io.tmpdir") ?: "/data/local/tmp"
            File(tmpDir, "autodev")
        } else {
            // 桌面环境：使用用户主目录
            File(homeDir, ".autodev")
        }

        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        val dbFile = File(dbDir, "autodev.db")
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            properties = Properties(),
            schema = DevInsDatabase.Schema
        )

        return driver
    }
}





