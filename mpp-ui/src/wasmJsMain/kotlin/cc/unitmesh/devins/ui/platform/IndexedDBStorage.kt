package cc.unitmesh.devins.ui.platform

import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

/**
 * External declarations for IndexedDB API
 */
external interface IDBFactory : JsAny {
    fun open(name: String, version: Int): IDBOpenDBRequest
}

external interface IDBOpenDBRequest : IDBRequest {
    var onupgradeneeded: ((IDBVersionChangeEvent) -> Unit)?
}

external interface IDBRequest : JsAny {
    val result: JsAny?
    val error: JsAny?
    var onsuccess: ((JsAny) -> Unit)?
    var onerror: ((JsAny) -> Unit)?
}

external interface IDBVersionChangeEvent : JsAny {
    val target: IDBOpenDBRequest
}

external interface IDBDatabase : JsAny {
    fun transaction(storeNames: JsString, mode: String): IDBTransaction
    fun createObjectStore(name: String): IDBObjectStore
}

external interface IDBTransaction : JsAny {
    fun objectStore(name: String): IDBObjectStore
}

external interface IDBObjectStore : JsAny {
    fun get(key: String): IDBRequest
    fun put(value: JsAny, key: String): IDBRequest
}

// Get the global indexedDB
private val indexedDB: IDBFactory = js("self.indexedDB")
private fun jsUndefined(): JsAny = js("undefined")

/**
 * IndexedDB storage wrapper for WASM platform
 * Provides binary data storage using IndexedDB
 */
object IndexedDBStorage {
    private const val DB_NAME = "DevInsDatabase"
    private const val STORE_NAME = "sqliteDb"
    private const val DB_VERSION = 1
    
    private var dbPromise: Promise<IDBDatabase>? = null
    
    /**
     * Get or create the database connection
     */
    private fun getDatabase(): Promise<IDBDatabase> {
        if (dbPromise != null) {
            return dbPromise!!
        }
        
        dbPromise = Promise { resolve, reject ->
            val request = indexedDB.open(DB_NAME, DB_VERSION)
            
            request.onupgradeneeded = { event ->
                val db = event.target.result?.unsafeCast<IDBDatabase>()
                db?.createObjectStore(STORE_NAME)
                println("IndexedDB: Created object store '$STORE_NAME'")
            }
            
            request.onsuccess = { _ ->
                val db = request.result?.unsafeCast<IDBDatabase>()
                if (db != null) {
                    println("IndexedDB: Database opened successfully")
                    resolve(db)
                } else {
                    reject("Failed to open database: result is null".toJsString())
                }
            }
            
            request.onerror = { _ ->
                println("IndexedDB: Error opening database: ${request.error}")
                reject(request.error ?: "Failed to open database".toJsString())
            }
        }
        
        return dbPromise!!
    }
    
    /**
     * Save binary data to IndexedDB
     */
    fun saveBinary(key: String, data: Uint8Array): Promise<JsAny> {
        return Promise { resolve, reject ->
            getDatabase().then { db ->
                val transaction = db.transaction(STORE_NAME.toJsString(), "readwrite")
                val store = transaction.objectStore(STORE_NAME)
                val request = store.put(data, key)
                
                request.onsuccess = { _ ->
                    println("IndexedDB: Saved data for key '$key' (${data.length} bytes)")
                    resolve(jsUndefined())
                }
                
                request.onerror = { _ ->
                    println("IndexedDB: Error saving data: ${request.error}")
                    reject(request.error ?: "Failed to save data".toJsString())
                }
                
                null
            }.catch { error ->
                println("IndexedDB: saveBinary error: $error")
                reject(error)
                null
            }
        }
    }
    
    /**
     * Load binary data from IndexedDB
     */
    fun loadBinary(key: String): Promise<Uint8Array?> {
        return Promise { resolve, reject ->
            getDatabase().then { db ->
                val transaction = db.transaction(STORE_NAME.toJsString(), "readonly")
                val store = transaction.objectStore(STORE_NAME)
                val request = store.get(key)
                
                request.onsuccess = { _ ->
                    val result = request.result
                    if (result != null) {
                        val data = result.unsafeCast<Uint8Array>()
                        println("IndexedDB: Loaded data for key '$key' (${data.length} bytes)")
                        resolve(data)
                    } else {
                        println("IndexedDB: No data found for key '$key'")
                        resolve(null)
                    }
                }
                
                request.onerror = { _ ->
                    println("IndexedDB: Error loading data: ${request.error}")
                    reject(request.error ?: "Failed to load data".toJsString())
                }
                
                null
            }.catch { error ->
                println("IndexedDB: loadBinary error: $error")
                reject(error)
                null
            }
        }
    }
}
