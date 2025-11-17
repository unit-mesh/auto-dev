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
 * LibGit2 Module configuration
 */
external interface LibGit2Config : JsAny {
    var locateFile: ((String) -> String)?
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
 * Console for logging (already defined in common wasm runtime)
 */
@JsName("console")
external object WasmConsole : JsAny {
    fun log(message: String)
    fun error(message: String)
    fun warn(message: String)
}

/**
 * Dynamic import for ES modules
 */
external interface WasmGitModule : JsAny {
    /**
     * Load the lg2 module
     */
    fun lg2(config: LibGit2Config? = definedExternally): Promise<LibGit2Module>
}

/**
 * Import wasm-git module
 */
@JsModule("wasm-git")
external val wasmGit: WasmGitModule

/**
 * Helper to create JS array
 */
fun jsArrayOf(vararg elements: String): JsArray<JsString> {
    val array = JsArray<JsString>()
    elements.forEach { array[array.length] = it.toJsString() }
    return array
}

/**
 * Helper to create LibGit2 config
 */
fun createLibGit2Config(cdnUrl: String = "https://unpkg.com/wasm-git@0.0.13/"): LibGit2Config {
    return createLibGit2ConfigInternal(cdnUrl)
}

/**
 * Internal function to create config using external declaration
 */
private external fun createLibGit2ConfigInternal(cdnUrl: String): LibGit2Config

/**
 * Helper to create JS object for file read options
 */
external interface FileReadOptions : JsAny {
    var encoding: String
}

/**
 * Create file read options
 */
fun createReadFileOptions(encoding: String = "utf8"): FileReadOptions {
    return createFileReadOptionsInternal(encoding)
}

/**
 * Internal function to create file read options
 */
private external fun createFileReadOptionsInternal(encoding: String): FileReadOptions


