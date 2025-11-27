package cc.unitmesh.devins.document.docql

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException
import cc.unitmesh.agent.tool.impl.initialMaxResults
import cc.unitmesh.devins.document.DocumentChunk
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.TOCItem

/**
 * DocQL query execution result - all results include source file information
 */
sealed class DocQLResult {
    /**
     * TOC items result - grouped by source file
     * Map key: file path, value: list of TOC items from that file
     */
    data class TocItems(val itemsByFile: Map<String, List<TOCItem>>) : DocQLResult() {
        val totalCount: Int get() = itemsByFile.values.sumOf { it.size }

        /** Flattened list of all items (for convenience in single-file tests) */
        val items: List<TOCItem> get() = itemsByFile.values.flatten()
    }

    /**
     * Entity items result - grouped by source file
     * Map key: file path, value: list of entities from that file
     */
    data class Entities(val itemsByFile: Map<String, List<Entity>>) : DocQLResult() {
        val totalCount: Int get() = itemsByFile.values.sumOf { it.size }

        /** Flattened list of all items (for convenience in single-file tests) */
        val items: List<Entity> get() = itemsByFile.values.flatten()
    }

    /**
     * Document chunks result - grouped by source file
     * Map key: file path, value: list of chunks from that file
     */
    data class Chunks(val itemsByFile: Map<String, List<DocumentChunk>>) : DocQLResult() {
        val totalCount: Int get() = itemsByFile.values.sumOf { it.size }

        /** Flattened list of all items (for convenience in single-file tests) */
        val items: List<DocumentChunk> get() = itemsByFile.values.flatten()
    }

    /**
     * Code blocks result - grouped by source file
     * Map key: file path, value: list of code blocks from that file
     */
    data class CodeBlocks(val itemsByFile: Map<String, List<CodeBlock>>) : DocQLResult() {
        val totalCount: Int get() = itemsByFile.values.sumOf { it.size }

        /** Flattened list of all items (for convenience in single-file tests) */
        val items: List<CodeBlock> get() = itemsByFile.values.flatten()
    }

    /**
     * Tables result - grouped by source file
     * Map key: file path, value: list of tables from that file
     */
    data class Tables(val itemsByFile: Map<String, List<TableBlock>>) : DocQLResult() {
        val totalCount: Int get() = itemsByFile.values.sumOf { it.size }

        /** Flattened list of all items (for convenience in single-file tests) */
        val items: List<TableBlock> get() = itemsByFile.values.flatten()
    }

    /**
     * File list result (for $.files queries)
     * Includes file metadata and optionally content
     */
    data class Files(val items: List<FileInfo>) : DocQLResult()

    /**
     * File structure result (for $.structure queries)
     * Provides a tree view of files and directories
     */
    data class Structure(
        val tree: String,
        val paths: List<String>,
        val directoryCount: Int,
        val fileCount: Int
    ) : DocQLResult()

    /**
     * Empty result
     */
    object Empty : DocQLResult()

    /**
     * Error result
     */
    data class Error(val message: String) : DocQLResult()

    fun getResultCount(): Int {
        return when (val result = this) {
            is DocQLResult.Entities -> result.totalCount
            is DocQLResult.TocItems -> result.totalCount
            is DocQLResult.Chunks -> result.totalCount
            is DocQLResult.CodeBlocks -> result.totalCount
            is DocQLResult.Tables -> result.totalCount
            is DocQLResult.Files -> result.items.size
            is DocQLResult.Structure -> result.fileCount
            else -> 0
        }
    }


    fun isEmptyResult(): Boolean {
        return when (val result = this) {
            is DocQLResult.Empty -> true
            is DocQLResult.TocItems -> result.totalCount == 0
            is DocQLResult.Entities -> result.totalCount == 0
            is DocQLResult.Chunks -> result.totalCount == 0
            is DocQLResult.CodeBlocks -> result.totalCount == 0
            is DocQLResult.Tables -> result.totalCount == 0
            is DocQLResult.Files -> result.items.isEmpty()
            is DocQLResult.Structure -> result.paths.isEmpty()
            is DocQLResult.Error -> true
        }
    }

    fun formatDocQLResult(maxResults: Int = initialMaxResults): String {
        return when (val result = this) {
            is DocQLResult.TocItems -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults * 2

                    appendLine("Found $totalItems TOC items across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                    }
                    appendLine()

                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break

                        appendLine("## $filePath")
                        for (item in items) {
                            if (count >= maxResults) break
                            appendLine("  ${"  ".repeat(item.level - 1)}${item.level}. ${item.title}")
                            count++
                        }
                        appendLine()
                    }

                    if (truncated) {
                        appendLine("💡 Tip: Query specific directories to get more focused results:")
                        appendLine("   \$.content.h1()")
                        appendLine("   \$.toc[?(@.title contains \"keyword\")]")
                    }
                }
            }

            is DocQLResult.Entities -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults

                    appendLine("Found $totalItems entities across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                    }
                    appendLine()

                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break

                        appendLine("## $filePath")
                        for (entity in items) {
                            if (count >= maxResults) break
                            when (entity) {
                                is Entity.ClassEntity -> {
                                    val pkg =
                                        if (!entity.packageName.isNullOrEmpty()) " (${entity.packageName})" else ""
                                    appendLine("  class ${entity.name}$pkg")
                                    if (entity.location.line != null) {
                                        appendLine("     └─ Line ${entity.location.line}")
                                    }
                                }

                                is Entity.FunctionEntity -> {
                                    val sig = entity.signature ?: entity.name
                                    appendLine("  ⚡ $sig")
                                    if (entity.location.line != null) {
                                        appendLine("     └─ Line ${entity.location.line}")
                                    }
                                }

                                is Entity.Term -> {
                                    appendLine("  📝 ${entity.name}: ${entity.definition ?: ""}")
                                }

                                is Entity.API -> {
                                    appendLine("  🔌 ${entity.name}: ${entity.signature ?: ""}")
                                }
                            }
                            count++
                        }
                        appendLine()
                    }

                    if (truncated) {
                        appendLine("💡 Tip: Use $.code.class(\"ClassName\") or $.code.function(\"functionName\") to get full source code")
                    }
                }
            }

            is DocQLResult.Chunks -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults

                    appendLine("Found $totalItems content chunks across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                        appendLine("💡 Tip: Narrow down your search to specific files or directories")
                        appendLine("   Example: Query documents in a specific directory only")
                    }
                    appendLine()

                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break

                        // Filter out empty or whitespace-only chunks
                        val nonEmptyItems = items.filter { it.content.trim().isNotEmpty() }
                        if (nonEmptyItems.isEmpty()) continue

                        appendLine("## 📄 $filePath")
                        appendLine()

                        for (chunk in nonEmptyItems) {
                            if (count >= maxResults) break

                            val content = chunk.content.trim()
                            if (content.isNotEmpty()) {
                                appendLine(content)
                                appendLine()
                                appendLine("---")
                                appendLine()
                                count++
                            }
                        }
                    }

                    if (count == 0) {
                        appendLine("No non-empty chunks found.")
                    }
                }
            }

            is DocQLResult.CodeBlocks -> {
                buildString {
                    appendLine("Found ${result.totalCount} code blocks across ${result.itemsByFile.size} file(s):")
                    appendLine()
                    for ((filePath, items) in result.itemsByFile) {
                        appendLine("## 📄 $filePath")
                        items.forEach { block ->
                            appendLine("```${block.language ?: ""}")
                            appendLine(block.code)
                            appendLine("```")
                            appendLine()
                        }
                    }
                }
            }

            is DocQLResult.Tables -> {
                buildString {
                    appendLine("Found ${result.totalCount} tables across ${result.itemsByFile.size} file(s):")
                    for ((filePath, items) in result.itemsByFile) {
                        appendLine("## 📄 $filePath")
                        appendLine("  ${items.size} table(s)")
                    }
                }
            }

            is DocQLResult.Files -> {
                buildString {
                    appendLine("Found ${result.items.size} files:")
                    appendLine()
                    result.items.forEach { file ->
                        appendLine("📄 ${file.path}")
                        if (file.directory.isNotEmpty()) {
                            appendLine("   Directory: ${file.directory}")
                        }
                        if (file.extension.isNotEmpty()) {
                            appendLine("   Type: ${file.extension}")
                        }
                        if (file.size > 0) {
                            appendLine("   Size: ${file.size} characters")
                        }
                        appendLine()
                    }
                    if (result.items.size > 50) {
                        appendLine("💡 Too many results! Consider filtering by directory:")
                        appendLine("   \$.files[?(@.path contains \"your-directory\")]")
                    }
                }
            }

            is DocQLResult.Structure -> {
                buildString {
                    appendLine("File Structure (${result.directoryCount} directories, ${result.fileCount} files):")
                    appendLine()
                    appendLine(result.tree)
                    appendLine()
                    appendLine("💡 Use \$.files[*] to get detailed file information")
                    appendLine("   \$.files[?(@.extension==\"kt\")] to filter by extension")
                }
            }

            is DocQLResult.Empty -> {
                "No results found."
            }

            is DocQLResult.Error -> {
                throw ToolException(result.message, ToolErrorType.COMMAND_FAILED)
            }
        }
    }
}