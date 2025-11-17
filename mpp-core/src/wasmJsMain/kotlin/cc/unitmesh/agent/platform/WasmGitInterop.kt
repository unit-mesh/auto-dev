package cc.unitmesh.agent.platform

import kotlin.js.Promise

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
    fun readFile(path: String, options: JsAny?): String
    
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
 * LibGit2 Module
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
    fun callMain(args: JsArray<JsString>): Int
}

/**
 * Console for logging
 */
external object console : JsAny {
    fun log(message: String)
    fun error(message: String)
    fun warn(message: String)
}

/**
 * Dynamic import for ES modules
 */
@JsModule("wasm-git")
external object WasmGit {
    /**
     * Load the lg2 module
     */
    fun lg2(config: JsAny?): Promise<LibGit2Module>
}

/**
 * Helper to create JS array
 */
fun jsArrayOf(vararg elements: String): JsArray<JsString> {
    val array = JsArray<JsString>()
    elements.forEach { array[array.length] = it.toJsString() }
    return array
}

/**
 * Helper to create JS object for file read options
 */
fun createReadFileOptions(encoding: String = "utf8"): JsAny {
    return js("({ encoding: encoding })").unsafeCast<JsAny>()
}

