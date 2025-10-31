package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val homeDir = System.getProperty("user.home")
        val dbDir = File(homeDir, ".autodev")
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




