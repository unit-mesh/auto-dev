package cc.unitmesh.devins.db

import kotlin.js.Promise
import org.khronos.webgl.Uint8Array

@JsModule("sql.js")
external fun initSqlJs(config: SqlJsConfig): Promise<SqlJs>

external interface SqlJsConfig : JsAny {
    var locateFile: (filename: String) -> String
}

external interface SqlJs : JsAny {
    val Database: DatabaseConstructor
}

external interface DatabaseConstructor : JsAny

// Top-level helper functions to call the Database constructor with 'new'
private fun createDatabaseWithData(constructor: DatabaseConstructor, data: Uint8Array): Database =
    js("new constructor(data)")

private fun createEmptyDatabase(constructor: DatabaseConstructor): Database =
    js("new constructor()")

fun DatabaseConstructor.create(data: Uint8Array? = null): Database {
    return if (data != null) {
        createDatabaseWithData(this, data)
    } else {
        createEmptyDatabase(this)
    }
}

external interface Database : JsAny {
    fun run(sql: String): Database
    fun run(sql: String, params: JsArray<JsAny?>): Database
    fun exec(sql: String): JsArray<QueryResults>
    fun prepare(sql: String): Statement
    fun prepare(sql: String, params: JsArray<JsAny?>): Statement
    fun export(): Uint8Array
    fun close()
    fun getRowsModified(): Int
}

external interface Statement : JsAny {
    fun bind(values: JsArray<JsAny?>): Boolean
    fun step(): Boolean
    fun get(): JsArray<JsAny?>
    fun getColumnNames(): JsArray<JsString>
    fun free()
    fun getAsObject(params: JsArray<JsAny?>? = definedExternally): JsAny
    fun run(params: JsArray<JsAny?>? = definedExternally)
}

external interface QueryResults : JsAny {
    val columns: JsArray<JsString>
    val values: JsArray<JsArray<JsAny?>>
}
