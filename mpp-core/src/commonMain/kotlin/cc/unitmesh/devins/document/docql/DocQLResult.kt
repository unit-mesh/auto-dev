package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.DocumentChunk
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.TOCItem

const val initialMaxResults = 20

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
     * Frontmatter result (for $.frontmatter queries)
     * Contains parsed YAML frontmatter data
     */
    data class Frontmatter(val data: Map<String, Any>) : DocQLResult()

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
            is DocQLResult.Frontmatter -> result.data.isEmpty()
            is DocQLResult.Error -> true
        }
    }
}

