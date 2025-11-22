package cc.unitmesh.devins.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import cc.unitmesh.devins.ui.platform.IndexedDBStorage
import org.khronos.webgl.Uint8Array
import kotlin.js.toJsString

private fun createEmptyConfig(): SqlJsConfig = js("({})")
private fun setUint8Array(array: Uint8Array, index: Int, value: Byte): Unit = js("array[index] = value")
private fun getUint8Array(array: Uint8Array, index: Int): Byte = js("array[index]")
private fun getUint8ArrayLength(array: Uint8Array): Int = js("array.length")

class IndexedDBSqlDriver(
    private val dbName: String = "devins.db"
) : SqlDriver {

    private var db: Database? = null
    private val listeners = mutableMapOf<String, MutableSet<app.cash.sqldelight.Query.Listener>>()
    private var initCallback: (() -> Unit)? = null

    // Queue for commands that happen before DB is ready
    private val commandQueue = mutableListOf<() -> Unit>()

    fun onInit(callback: () -> Unit) {
        if (db != null) {
            callback()
        } else {
            val prev = initCallback
            initCallback = {
                prev?.invoke()
                callback()
            }
        }
    }

    init {
        println("LocalStorageSqlDriver: Initializing...")
        val config = createEmptyConfig()
        config.locateFile = { filename ->
            println("LocalStorageSqlDriver: locateFile called for $filename")
            if (filename.endsWith(".wasm")) {
                val path = "sql-wasm.wasm"
                println("LocalStorageSqlDriver: Resolved WASM path to $path")
                path
            } else {
                filename
            }
        }

        println("LocalStorageSqlDriver: Calling initSqlJs...")
        initSqlJs(config).then { sqlJs ->
            println("LocalStorageSqlDriver: initSqlJs resolved successfully")

            // Load from IndexedDB
            IndexedDBStorage.loadBinary(dbName).then { savedDb ->
                if (savedDb != null) {
                    try {
                        db = sqlJs.Database.create(savedDb)
                        println("Loaded database from IndexedDB (size: ${savedDb.length} bytes)")
                    } catch (e: Exception) {
                        println("Failed to load database: ${e.message}")
                        db = sqlJs.Database.create()
                    }
                } else {
                    db = sqlJs.Database.create()
                    println("Created new database (no existing data in IndexedDB)")
                }

                // Process queued commands
                if (commandQueue.isNotEmpty()) {
                    println("Processing ${commandQueue.size} queued commands...")
                    commandQueue.forEach { it() }
                    commandQueue.clear()
                    println("Queued commands processed.")
                }

                initCallback?.invoke()
                initCallback = null

                // Notify all listeners to refresh data
                listeners.values.flatten().forEach { it.queryResultsChanged() }

                null
            }.catch { error ->
                println("Failed to load from IndexedDB: $error")
                db = sqlJs.Database.create()
                println("Created new database after IndexedDB error")

                // Still process queued commands even if load failed
                if (commandQueue.isNotEmpty()) {
                    println("Processing ${commandQueue.size} queued commands...")
                    commandQueue.forEach { it() }
                    commandQueue.clear()
                }

                initCallback?.invoke()
                initCallback = null
                listeners.values.flatten().forEach { it.queryResultsChanged() }

                null
            }

            null
        }.catch { error ->
            println("FAILED to initialize sql.js: $error")
            null
        }
    }

    private fun getDb(): Database? {
        return db
    }

    private fun save() {
        val database = db ?: return
        try {
            val binary = database.export()
            IndexedDBStorage.saveBinary(dbName, binary).then {
                val length = getUint8ArrayLength(binary)
                println("Saved database to IndexedDB (size: $length bytes)")
                null
            }.catch { error ->
                println("Failed to save database to IndexedDB: $error")
                null
            }
        } catch (e: Exception) {
            println("Failed to export database: ${e.message}")
        }
    }

    override fun addListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {
        queryKeys.forEach { key ->
            listeners.getOrPut(key) { mutableSetOf() }.add(listener)
        }
    }

    override fun close() {
        db?.close()
    }

    override fun currentTransaction(): app.cash.sqldelight.Transacter.Transaction? = null

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<Long> {
        val database = getDb()

        if (database == null) {
            println("Database not ready, queuing execution: $sql")
            commandQueue.add {
                execute(identifier, sql, parameters, binders)
            }
            return QueryResult.Value(0L)
        }

        val statement = database.prepare(sql)

        try {
            val boundValues = JsArray<JsAny?>()
            val binder = object : SqlPreparedStatement {
                override fun bindBoolean(index: Int, boolean: Boolean?) {
                    boundValues[index - 1] = boolean?.toJsBoolean()
                }

                override fun bindBytes(index: Int, bytes: ByteArray?) {
                    // TODO: Handle bytes binding if needed, complex in Wasm
                    boundValues[index - 1] = null
                }

                override fun bindDouble(index: Int, double: Double?) {
                    boundValues[index - 1] = double?.toJsNumber()
                }

                override fun bindLong(index: Int, long: Long?) {
                    boundValues[index - 1] = long?.toDouble()?.toJsNumber()
                }

                override fun bindString(index: Int, string: String?) {
                    boundValues[index - 1] = string?.toJsString()
                }
            }
            binders?.invoke(binder)

            if (boundValues.length > 0) {
                statement.bind(boundValues)
            }

            statement.step()
            val changes = database.getRowsModified().toLong()
            statement.free()

            save() // Auto-save after write
            notifyListeners(identifier.toString()) // Simplified notification

            return QueryResult.Value(changes)
        } catch (e: Exception) {
            statement.free()
            throw e
        }
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<R> {
        val database = getDb()

        if (database == null) {
            println("Database not ready, returning empty result for query: $sql")
            // Return empty cursor
            val emptyCursor = object : SqlCursor {
                override fun next(): QueryResult<Boolean> = QueryResult.Value(false)
                override fun getString(index: Int): String? = null
                override fun getLong(index: Int): Long? = null
                override fun getBytes(index: Int): ByteArray? = null
                override fun getDouble(index: Int): Double? = null
                override fun getBoolean(index: Int): Boolean? = null
            }
            return mapper(emptyCursor)
        }

        val statement = database.prepare(sql)

        val boundValues = JsArray<JsAny?>()
        val binder = object : SqlPreparedStatement {
            override fun bindBoolean(index: Int, boolean: Boolean?) {
                boundValues[index - 1] = boolean?.toJsBoolean()
            }

            override fun bindBytes(index: Int, bytes: ByteArray?) {
                boundValues[index - 1] = null
            }

            override fun bindDouble(index: Int, double: Double?) {
                boundValues[index - 1] = double?.toJsNumber()
            }

            override fun bindLong(index: Int, long: Long?) {
                boundValues[index - 1] = long?.toDouble()?.toJsNumber()
            }

            override fun bindString(index: Int, string: String?) {
                boundValues[index - 1] = string?.toJsString()
            }
        }
        binders?.invoke(binder)

        if (boundValues.length > 0) {
            statement.bind(boundValues)
        }

        val cursor = object : SqlCursor {
            override fun next(): QueryResult<Boolean> {
                return QueryResult.Value(statement.step())
            }

            override fun getString(index: Int): String? = (statement.get()[index] as? JsString)?.toString()
            override fun getLong(index: Int): Long? = (statement.get()[index] as? JsNumber)?.toDouble()?.toLong()
            override fun getBytes(index: Int): ByteArray? = null // Not implemented
            override fun getDouble(index: Int): Double? = (statement.get()[index] as? JsNumber)?.toDouble()
            override fun getBoolean(index: Int): Boolean? = (statement.get()[index] as? JsBoolean)?.toBoolean()
        }

        val result = mapper(cursor)
        statement.free()

        return result
    }

    override fun newTransaction(): QueryResult<app.cash.sqldelight.Transacter.Transaction> {
        val transaction = object : app.cash.sqldelight.Transacter.Transaction() {
            override val enclosingTransaction: app.cash.sqldelight.Transacter.Transaction? = null
            override fun endTransaction(successful: Boolean): QueryResult<Unit> {
                return QueryResult.Value(Unit)
            }
        }
        return QueryResult.Value(transaction)
    }

    override fun notifyListeners(vararg queryKeys: String) {
        queryKeys.forEach { key ->
            listeners[key]?.forEach { listener ->
                listener.queryResultsChanged()
            }
        }
    }

    override fun removeListener(vararg queryKeys: String, listener: app.cash.sqldelight.Query.Listener) {
        queryKeys.forEach { key ->
            listeners[key]?.remove(listener)
        }
    }
}
