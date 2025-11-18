package cc.unitmesh.agent.platform

import kotlin.js.Promise

/**
 * Module configuration for wasm-git
 */
external interface ModuleConfig : JsAny {
    var print: ((String) -> Unit)?
    var printErr: ((String) -> Unit)?
    var onRuntimeInitialized: (() -> Unit)?
}

/**
 * Import wasm-git/lg2_async.js as default export
 * lg2.js exports an async function: async function(moduleArg = {})
 *
 * https://raw.githubusercontent.com/petersalomonsen/githttpserver/refs/heads/master/public/libgit2_webworker.js
 */
@JsModule("wasm-git/lg2_async.js")
external fun lg2(config: ModuleConfig? = definedExternally): Promise<LibGit2Module>

/**
 * LibGit2 Module - returned by lg2() function
 */
external interface LibGit2Module : JsAny {
    val FS: EmscriptenFS
    
    /**
     * Called when WASM runtime is initialized
     */
    var onRuntimeInitialized: (() -> Unit)?
    
    /**
     * Call git command with arguments
     * Returns exit code (0 for success)
     */
    fun callMain(args: JsArray<JsString>): Promise<JsNumber>
}

/**
 * Emscripten File System API
 */
external interface EmscriptenFS : JsAny {
    /**
     * Write a file to the virtual file system
     */
    fun writeFile(path: String, data: String)
    
    /**
     * Read a file from the virtual file system
     */
    fun readFile(path: String): JsArray<JsNumber>
    
    /**
     * Read directory contents
     */
    fun readdir(path: String): JsArray<JsString>
    
    /**
     * Create a directory
     */
    fun mkdir(path: String)
    
    /**
     * Change current directory
     */
    fun chdir(path: String)
    
    /**
     * Sync file system with IndexedDB
     * @param populate true to load from IndexedDB, false to save to IndexedDB
     * @param callback callback function
     */
    fun syncfs(populate: Boolean, callback: () -> Unit)
}

/**
 * Console for logging
 */
@JsName("console")
external object WasmConsole : JsAny {
    fun log(message: String)
    fun error(message: String)
    fun warn(message: String)
}

/**
 * Helper to create JS array of strings
 */
fun jsArrayOf(vararg elements: String): JsArray<JsString> {
    val array = JsArray<JsString>()
    elements.forEach { array[array.length] = it.toJsString() }
    return array
}

/**
 * Helper extension to convert JsArray to Kotlin List
 */
fun <T : JsAny> JsArray<T>.toList(): List<T> {
    val result = mutableListOf<T>()
    for (i in 0 until this.length) {
        val item = this[i]
        if (item != null) {
            result.add(item)
        }
    }
    return result
}

/**
 * Convert JsArray<JsNumber> to IntArray
 */
fun JsArray<JsNumber>.toIntArray(): IntArray {
    return IntArray(this.length) { this[it]?.toInt() ?: 0 }
}

private val _emptyJsObject: JsAny = js("({})")

/**
 * Create a Module configuration object for wasm-git
 */
fun createModuleConfig(
    onPrint: ((String) -> Unit)? = null,
    onPrintErr: ((String) -> Unit)? = null,
    onRuntimeInitialized: (() -> Unit)? = null
): ModuleConfig {
    val config = _emptyJsObject.unsafeCast<ModuleConfig>()
    if (onPrint != null) config.print = onPrint
    if (onPrintErr != null) config.printErr = onPrintErr
    if (onRuntimeInitialized != null) config.onRuntimeInitialized = onRuntimeInitialized
    return config
}


