package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = createDefaultWebWorkerDriver()
        return driver
    }
}
