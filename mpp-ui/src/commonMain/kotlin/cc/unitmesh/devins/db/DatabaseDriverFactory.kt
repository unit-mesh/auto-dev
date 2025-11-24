package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult

/**
 * 跨平台的数据库驱动工厂
 * 每个平台需要提供自己的实现
 */
expect class DatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}

/**
 * 检查列是否存在
 */
private fun columnExists(driver: SqlDriver, tableName: String, columnName: String): Boolean {
    return try {
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($tableName)",
            mapper = { cursor ->
                var exists = false
                while (cursor.next().value) {
                    val name = cursor.getString(1)
                    if (name == columnName) {
                        exists = true
                        break
                    }
                }
                QueryResult.Value(exists)
            },
            parameters = 0,
            binders = null
        ).value
    } catch (e: Exception) {
        false
    }
}

/**
 * 创建数据库实例并应用迁移
 */
fun createDatabase(driverFactory: DatabaseDriverFactory): DevInsDatabase {
    val driver = driverFactory.createDriver()
    
    // 获取当前数据库版本
    val currentVersion = try {
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor ->
                if (cursor.next().value) {
                    QueryResult.Value(cursor.getLong(0)?.toInt() ?: 0)
                } else {
                    QueryResult.Value(0)
                }
            },
            parameters = 0,
            binders = null
        ).value
    } catch (e: Exception) {
        0
    }
    
    // 如果是全新数据库（version = 0），直接创建最新 schema
    if (currentVersion == 0) {
        DevInsDatabase.Schema.create(driver)
        driver.execute(
            identifier = null,
            sql = "PRAGMA user_version = ${DevInsDatabase.Schema.version}",
            parameters = 0,
            binders = null
        )
    } else if (currentVersion < DevInsDatabase.Schema.version.toInt()) {
        // 对于已存在的数据库，只添加缺失的列
        // Migration 2: 添加 planOutput 列（如果不存在）
        if (!columnExists(driver, "CodeReviewAnalysisDb", "planOutput")) {
            driver.execute(
                identifier = null,
                sql = "ALTER TABLE CodeReviewAnalysisDb ADD COLUMN planOutput TEXT NOT NULL DEFAULT ''",
                parameters = 0,
                binders = null
            )
        }
        
        // 更新版本号
        driver.execute(
            identifier = null,
            sql = "PRAGMA user_version = ${DevInsDatabase.Schema.version}",
            parameters = 0,
            binders = null
        )
    }
    
    return DevInsDatabase(driver)
}
