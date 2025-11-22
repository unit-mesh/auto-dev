package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = createDefaultWebWorkerDriver()
        return driver
    }

    /// not working but hard to debug
//    actual fun createDriver(): SqlDriver {
//        return IndexedDBSqlDriver().also { driver ->
//            (driver as? IndexedDBSqlDriver)?.onInit {
//                try {
//                    DevInsDatabase.Schema.create(driver)
//                } catch (e: Exception) {
//                    // Ignore if tables already exist (if Schema.create doesn't use IF NOT EXISTS)
//                    println("Schema creation result: ${e.message}")
//                }
//            }
//        }
//    }
}
