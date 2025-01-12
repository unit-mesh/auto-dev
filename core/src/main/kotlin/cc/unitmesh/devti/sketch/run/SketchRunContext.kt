package cc.unitmesh.devti.sketch.run

import com.intellij.openapi.vfs.VirtualFile

data class SketchRunContext(
    // Current File
    @JvmField val file: VirtualFile,
    /// related files
    @JvmField val userSelectFile: List<VirtualFile>,
    /// ast related files
    @JvmField val astRelatedFiles: List<VirtualFile>,
    // The absolute path of the USER's workspace
    @JvmField val workspace: String,
    // The USER's OS
    @JvmField val os: String,
    // The current time in YYYY-MM-DD HH:MM:SS format
    @JvmField val time: String,
    /// The USER's requirements
    @JvmField val input: String,
) {

}

/**
 * todo use [cc.unitmesh.devti.language.compiler.exec.InsCommand] to run the sketch
 */
enum class SketchToolchain(val toolName: String, val description: String) {
    RELATED_CODE("RelatedCode", "Find related code snippets across your codebase"),

    /// similar code search
    SIMILAR_CODE("SimilarCode", "Find similar code snippets based on semantic search"),

    /// text search
    GREP_SEARCH("GrepSearch", "Search for a specified pattern within files"),

    /// `Find`
    FIND("Find", "Search for files and directories using glob patterns"),

    /// TREE DIR
    TREE_DIR("TreeDir", "List files and directories in a tree-like structure"),

    /// VIEW FILE
    VIEW_FILE("ViewFile", "View the contents of a file"),

    /// VIEW SYMBOL
    VIEW_SYMBOL("ViewSymbol", "View file by symbol, like package, class, function, etc."),

    /// WRITE TO FILE
    WRITE_FILE("WriteFile", "Write to a file"),

    /// RUN COMMAND
    RUN_COMMAND("RunCommand", "Run a command in the terminal"),
}