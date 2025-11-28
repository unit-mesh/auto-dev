package cc.unitmesh.codegraph.model

import kotlinx.serialization.Serializable

/**
 * Represents an import statement in source code.
 * Extracted using TreeSitter for accurate multi-language support.
 * 
 * 导入信息模型：
 * - 支持多种语言的导入语法
 * - 提供精确的 AST 位置信息
 * - 可用于构建依赖图
 */
@Serializable
data class ImportInfo(
    /**
     * The full import path/name
     * Examples:
     * - Java: "java.util.List"
     * - Kotlin: "kotlin.collections.List"
     * - Python: "os.path"
     * - JavaScript: "./utils/helper"
     * - Go: "fmt"
     * - Rust: "std::collections::HashMap"
     */
    val path: String,
    
    /**
     * The import type
     */
    val type: ImportType,
    
    /**
     * The alias if the import is aliased (e.g., "import X as Y")
     */
    val alias: String? = null,
    
    /**
     * Specific names imported (for "from X import a, b" style)
     * Empty for wildcard or full module imports
     */
    val importedNames: List<String> = emptyList(),
    
    /**
     * Whether this is a wildcard import (e.g., "import java.util.*")
     */
    val isWildcard: Boolean = false,
    
    /**
     * Whether this is a static import (Java)
     */
    val isStatic: Boolean = false,
    
    /**
     * Source file path where this import is declared
     */
    val filePath: String,
    
    /**
     * Start line number (1-based)
     */
    val startLine: Int,
    
    /**
     * End line number (1-based)
     */
    val endLine: Int,
    
    /**
     * Raw import text from source
     */
    val rawText: String = ""
) {
    /**
     * Get the module/package part of the import
     * e.g., "java.util.List" -> "java.util"
     */
    fun getModulePath(): String {
        val separator = when {
            path.contains("::") -> "::"  // Rust
            path.contains("/") -> "/"    // JS relative
            else -> "."                   // Java/Kotlin/Python
        }
        val lastIndex = path.lastIndexOf(separator)
        return if (lastIndex > 0) path.substring(0, lastIndex) else path
    }
    
    /**
     * Get the simple name being imported
     * e.g., "java.util.List" -> "List"
     */
    fun getSimpleName(): String {
        val separator = when {
            path.contains("::") -> "::"
            path.contains("/") -> "/"
            else -> "."
        }
        val lastIndex = path.lastIndexOf(separator)
        return if (lastIndex >= 0) path.substring(lastIndex + separator.length) else path
    }
    
    /**
     * Check if this import could resolve to a given file path
     */
    fun couldResolveTo(targetFilePath: String): Boolean {
        // Convert import path to potential file path patterns
        val normalizedPath = path
            .replace(".", "/")
            .replace("::", "/")
            .removePrefix("@")
            .removeSuffix(".*")
        
        return targetFilePath.contains(normalizedPath) ||
               targetFilePath.endsWith("$normalizedPath.kt") ||
               targetFilePath.endsWith("$normalizedPath.java") ||
               targetFilePath.endsWith("$normalizedPath.py") ||
               targetFilePath.endsWith("$normalizedPath.js") ||
               targetFilePath.endsWith("$normalizedPath.ts") ||
               targetFilePath.endsWith("$normalizedPath.tsx")
    }
}

/**
 * Types of import statements
 */
@Serializable
enum class ImportType {
    /**
     * Standard module import (import X)
     */
    MODULE,
    
    /**
     * Selective import (from X import Y)
     */
    SELECTIVE,
    
    /**
     * Relative import (from . import X)
     */
    RELATIVE,
    
    /**
     * Package import (for languages with package concepts)
     */
    PACKAGE,
    
    /**
     * Side-effect import (import for side effects only)
     */
    SIDE_EFFECT,
    
    /**
     * Unknown import type
     */
    UNKNOWN
}

/**
 * Collection of imports from a single file
 */
@Serializable
data class FileImports(
    /**
     * File path
     */
    val filePath: String,
    
    /**
     * All imports in this file
     */
    val imports: List<ImportInfo>,
    
    /**
     * Package/module declaration of this file (if any)
     */
    val packageName: String = ""
) {
    /**
     * Get unique module paths being imported
     */
    fun getUniqueModules(): Set<String> {
        return imports.map { it.getModulePath() }.toSet()
    }
    
    /**
     * Get imports grouped by module
     */
    fun groupByModule(): Map<String, List<ImportInfo>> {
        return imports.groupBy { it.getModulePath() }
    }
}

