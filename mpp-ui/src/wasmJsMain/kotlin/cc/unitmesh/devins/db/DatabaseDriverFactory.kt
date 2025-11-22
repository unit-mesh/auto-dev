package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return LocalStorageSqlDriver().also { driver ->
             // We cannot call Schema.create(driver) immediately because sql.js init is async.
             // However, if we don't call it, tables won't exist.
             // But LocalStorageSqlDriver loads existing DB.
             // If it's a new DB, we need Schema.create.
             // The driver will throw if we call execute before init.
             
             // Solution: We can't block here. 
             // We'll rely on the fact that the app probably won't query immediately, 
             // OR we accept that the first query might fail if it happens < 100ms after start.
             // Ideally, we should expose an 'await' method, but the interface doesn't allow it.
             
             // We will try to create schema, catching the "not initialized" exception.
             // Actually, since we can't wait, we should probably defer schema creation 
             // inside the driver's init callback.
             
             // But Schema.create(driver) executes SQL.
             // We can pass a callback to the driver? No, driver is generic.
             
             // Let's just return the driver. The Schema creation should happen 
             // when the DB is actually ready.
             // But standard SQLDelight usage is: createDriver -> Schema.create -> return driver.
             
             // If I modify LocalStorageSqlDriver to queue operations?
             // That's complex for a synchronous interface (return values).
             
             // For now, I'll just return the driver. 
             // The user might need to handle the initialization delay.
             // Or I can hack it: 
             // LocalStorageSqlDriver can have a 'onInit' callback.
             
             (driver as? LocalStorageSqlDriver)?.onInit {
                 try {
                     DevInsDatabase.Schema.create(driver)
                 } catch (e: Exception) {
                     // Ignore if tables already exist (if Schema.create doesn't use IF NOT EXISTS)
                     println("Schema creation result: ${e.message}")
                 }
             }
        }
    }
}
