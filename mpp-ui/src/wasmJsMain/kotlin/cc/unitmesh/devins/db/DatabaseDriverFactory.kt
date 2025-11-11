package cc.unitmesh.devins.db

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // For WASM platform, we use a simple in-memory mock driver
        return MockSqlDriver().also { driver ->
            DevInsDatabase.Schema.create(driver)
        }
    }
}

/**
 * A simple mock SQL driver for WASM platform
 */
private class MockSqlDriver : SqlDriver {
    private val listeners = mutableMapOf<String, MutableSet<app.cash.sqldelight.Query.Listener>>()

    override fun close() {
        listeners.clear()
    }

    override fun currentTransaction(): app.cash.sqldelight.Transacter.Transaction? = null

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<Long> {
        return app.cash.sqldelight.db.QueryResult.Value(0L)
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> app.cash.sqldelight.db.QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): app.cash.sqldelight.db.QueryResult<R> {
        val mockCursor = object : SqlCursor {
            override fun getString(index: Int): String? = null
            override fun getLong(index: Int): Long? = null
            override fun getBytes(index: Int): ByteArray? = null
            override fun getDouble(index: Int): Double? = null
            override fun getBoolean(index: Int): Boolean? = null
            override fun next(): app.cash.sqldelight.db.QueryResult<Boolean> =
                app.cash.sqldelight.db.QueryResult.Value(false)
        }
        return mapper(mockCursor)
    }

    override fun newTransaction(): app.cash.sqldelight.db.QueryResult<app.cash.sqldelight.Transacter.Transaction> {
        val transaction = object : app.cash.sqldelight.Transacter.Transaction() {
            override val enclosingTransaction: app.cash.sqldelight.Transacter.Transaction? = null
            override fun endTransaction(successful: Boolean): app.cash.sqldelight.db.QueryResult<Unit> {
                return app.cash.sqldelight.db.QueryResult.Value(Unit)
            }
        }
        return app.cash.sqldelight.db.QueryResult.Value(transaction)
    }

    override fun addListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {
        queryKeys.forEach { key ->
            listeners.getOrPut(key) { mutableSetOf() }.add(listener)
        }
    }

    override fun removeListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {
        queryKeys.forEach { key ->
            listeners[key]?.remove(listener)
        }
    }

    override fun notifyListeners(vararg queryKeys: String) {
        queryKeys.forEach { key ->
            listeners[key]?.forEach { listener ->
                listener.queryResultsChanged()
            }
        }
    }
}

