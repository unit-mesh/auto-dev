package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

/**
 * JVM 平台的 SQLite 驱动工厂
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // 数据库文件存储在用户主目录下的 .devins 文件夹
        val homeDir = System.getProperty("user.home")
        val dbDir = File(homeDir, ".devins")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        
        val dbFile = File(dbDir, "devins.db")
        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            properties = Properties(),
            schema = DevInsDatabase.Schema
        )
        
        return driver
    }
}

