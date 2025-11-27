package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*

/**
 * Base class for DocQL executors - contains shared logic for all document types
 */
abstract class BaseDocQLExecutor(
    protected val documentFile: DocumentFile?,
    protected val parserService: DocumentParserService?
) {
    /**
     * Execute a DocQL query
     */
    suspend fun execute(query: DocQLQuery): DocQLResult {
        try {
            if (query.nodes.isEmpty()) {
                return DocQLResult.Error("Empty query")
            }

            // First node must be Root
            if (query.nodes[0] !is DocQLNode.Root) {
                return DocQLResult.Error("Query must start with $")
            }

            // Second node determines the context
            if (query.nodes.size < 2) {
                return DocQLResult.Error("Incomplete query")
            }

            return when (val contextNode = query.nodes[1]) {
                is DocQLNode.Property -> {
                    when (contextNode.name) {
                        "toc" -> executeTocQuery(query.nodes.drop(2))
                        "entities" -> executeEntitiesQuery(query.nodes.drop(2))
                        "content" -> executeContentQuery(query.nodes.drop(2))
                        "code" -> executeCodeQuery(query.nodes.drop(2))
                        "frontmatter" -> executeFrontmatterQuery(query.nodes.drop(2))
                        "files" -> DocQLResult.Error("$.files queries must be executed via DocumentRegistry.queryDocuments()")
                        "structure" -> executeStructureQuery(query.nodes.drop(2))
                        else -> DocQLResult.Error("Unknown context '${contextNode.name}'")
                    }
                }

                else -> DocQLResult.Error("Expected property after $")
            }
        } catch (e: Exception) {
            return DocQLResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Execute TOC query ($.toc[...])
     */
    protected fun executeTocQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        var items = documentFile.toc

        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all items (flatten tree)
                    items = flattenToc(items)
                }

                is DocQLNode.ArrayAccess.Index -> {
                    // Return specific index
                    items = if (node.index < items.size) {
                        listOf(items[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    // Filter items
                    items = filterTocItems(items, node.condition)
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for TOC query")
                }
            }
        }

        // Return result with source file information
        return if (items.isNotEmpty()) {
            DocQLResult.TocItems(mapOf(documentFile.path to items))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute entities query ($.entities[...])
     */
    protected fun executeEntitiesQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }

        var items = documentFile.entities

        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all entities
                }

                is DocQLNode.ArrayAccess.Index -> {
                    // Return specific index
                    items = if (node.index < items.size) {
                        listOf(items[node.index])
                    } else {
                        emptyList()
                    }
                }

                is DocQLNode.ArrayAccess.Filter -> {
                    // Filter entities
                    items = filterEntities(items, node.condition)
                }

                else -> {
                    return DocQLResult.Error("Invalid operation for entities query")
                }
            }
        }

        // Return result with source file information
        return if (items.isNotEmpty()) {
            DocQLResult.Entities(mapOf(documentFile.path to items))
        } else {
            DocQLResult.Empty
        }
    }

    /**
     * Execute structure query: $.structure, $.structure.tree(), $.structure.flat()
     * Returns a file structure view from the current document context.
     */
    protected fun executeStructureQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded. Use DocumentRegistry.queryDocuments() for $.structure queries across all documents.")
        }

        // For single document, return its path structure
        val paths = listOf(documentFile.path)
        val tree = buildFileTree(paths)

        return DocQLResult.Structure(
            tree = tree,
            paths = paths,
            directoryCount = countDirectories(paths),
            fileCount = paths.size
        )
    }

    /**
     * Execute content query - must be implemented by subclasses
     */
    protected abstract suspend fun executeContentQuery(nodes: List<DocQLNode>): DocQLResult

    /**
     * Execute code query - must be implemented by subclasses
     */
    protected abstract suspend fun executeCodeQuery(nodes: List<DocQLNode>): DocQLResult

    /**
     * Execute frontmatter query - must be implemented by subclasses
     */
    protected abstract suspend fun executeFrontmatterQuery(nodes: List<DocQLNode>): DocQLResult

    /**
     * Flatten TOC tree into a list
     */
    protected fun flattenToc(items: List<TOCItem>): List<TOCItem> {
        val result = mutableListOf<TOCItem>()
        for (item in items) {
            result.add(item)
            result.addAll(flattenToc(item.children))
        }
        return result
    }

    /**
     * Filter TOC items by condition
     */
    protected fun filterTocItems(items: List<TOCItem>, condition: FilterCondition): List<TOCItem> {
        val flattened = flattenToc(items)
        return flattened.filter { item ->
            when (condition) {
                is FilterCondition.Equals -> {
                    when (condition.property) {
                        "level" -> item.level.toString() == condition.value
                        "title" -> item.title == condition.value
                        else -> false
                    }
                }

                is FilterCondition.NotEquals -> {
                    when (condition.property) {
                        "level" -> item.level.toString() != condition.value
                        "title" -> item.title != condition.value
                        else -> true
                    }
                }

                is FilterCondition.Contains -> {
                    when (condition.property) {
                        "title" -> item.title.contains(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.RegexMatch -> {
                    when (condition.property) {
                        "title" -> matchesRegex(item.title, condition.pattern, condition.flags)
                        else -> false
                    }
                }

                is FilterCondition.GreaterThan -> {
                    when (condition.property) {
                        "level" -> item.level > condition.value
                        "page" -> (item.page ?: 0) > condition.value
                        else -> false
                    }
                }

                is FilterCondition.GreaterThanOrEquals -> {
                    when (condition.property) {
                        "level" -> item.level >= condition.value
                        "page" -> (item.page ?: 0) >= condition.value
                        else -> false
                    }
                }

                is FilterCondition.LessThan -> {
                    when (condition.property) {
                        "level" -> item.level < condition.value
                        "page" -> (item.page ?: 0) < condition.value
                        else -> false
                    }
                }

                is FilterCondition.LessThanOrEquals -> {
                    when (condition.property) {
                        "level" -> item.level <= condition.value
                        "page" -> (item.page ?: 0) <= condition.value
                        else -> false
                    }
                }

                is FilterCondition.StartsWith -> {
                    when (condition.property) {
                        "title" -> item.title.startsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.EndsWith -> {
                    when (condition.property) {
                        "title" -> item.title.endsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }
            }
        }
    }

    /**
     * Filter entities by condition
     */
    protected fun filterEntities(items: List<Entity>, condition: FilterCondition): List<Entity> {
        return items.filter { entity ->
            when (condition) {
                is FilterCondition.Equals -> {
                    when (condition.property) {
                        "name" -> entity.name == condition.value
                        "type" -> {
                            val entityType = when (entity) {
                                is Entity.Term -> "Term"
                                is Entity.API -> "API"
                                is Entity.ClassEntity -> "ClassEntity"
                                is Entity.FunctionEntity -> "FunctionEntity"
                                is Entity.ConstructorEntity -> "ConstructorEntity"
                            }
                            entityType == condition.value
                        }

                        else -> false
                    }
                }

                is FilterCondition.NotEquals -> {
                    when (condition.property) {
                        "name" -> entity.name != condition.value
                        "type" -> {
                            val entityType = when (entity) {
                                is Entity.Term -> "Term"
                                is Entity.API -> "API"
                                is Entity.ClassEntity -> "ClassEntity"
                                is Entity.FunctionEntity -> "FunctionEntity"
                                is Entity.ConstructorEntity -> "ConstructorEntity"
                            }
                            entityType != condition.value
                        }

                        else -> true
                    }
                }

                is FilterCondition.Contains -> {
                    when (condition.property) {
                        "name" -> entity.name.contains(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.RegexMatch -> {
                    when (condition.property) {
                        "name" -> matchesRegex(entity.name, condition.pattern, condition.flags)
                        else -> false
                    }
                }

                is FilterCondition.StartsWith -> {
                    when (condition.property) {
                        "name" -> entity.name.startsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.EndsWith -> {
                    when (condition.property) {
                        "name" -> entity.name.endsWith(condition.value, ignoreCase = true)
                        else -> false
                    }
                }

                is FilterCondition.GreaterThan, is FilterCondition.GreaterThanOrEquals,
                is FilterCondition.LessThan, is FilterCondition.LessThanOrEquals -> false
            }
        }
    }

    /**
     * Check if text matches regex pattern with flags
     * Note: Only IGNORE_CASE and MULTILINE are supported across all Kotlin platforms (JVM, JS, Native)
     * The 's' flag (DOT_MATCHES_ALL) is not available in Kotlin/JS, so we ignore it for cross-platform compatibility
     */
    protected fun matchesRegex(text: String, pattern: String, flags: String): Boolean {
        return try {
            val options = mutableSetOf<RegexOption>()
            if (flags.contains('i')) options.add(RegexOption.IGNORE_CASE)
            if (flags.contains('m')) options.add(RegexOption.MULTILINE)
            // Note: RegexOption.DOT_MATCHES_ALL is not available in Kotlin/JS
            // For cross-platform compatibility, we skip the 's' flag

            val regex = Regex(pattern, options)
            regex.containsMatchIn(text)
        } catch (e: Exception) {
            // Invalid regex pattern
            false
        }
    }

    /**
     * Build a tree-like string representation of file paths.
     */
    protected fun buildFileTree(paths: List<String>): String {
        return DocumentRegistry.buildStructureTree(paths)
    }

    /**
     * Count unique directories from file paths.
     */
    protected fun countDirectories(paths: List<String>): Int {
        return DocumentRegistry.countDirectories(paths)
    }
}
