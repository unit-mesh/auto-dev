package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlDriver

/**
 * 跨平台的数据库驱动工厂
 * 每个平台需要提供自己的实现
 */
expect class DatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}

/**
 * 创建数据库实例
 */
fun createDatabase(driverFactory: DatabaseDriverFactory): DevInsDatabase {
    val driver = driverFactory.createDriver()
    return DevInsDatabase(driver)
}





