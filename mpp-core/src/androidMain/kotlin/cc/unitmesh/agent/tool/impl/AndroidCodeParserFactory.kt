package cc.unitmesh.agent.tool.impl

import cc.unitmesh.codegraph.model.*
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language

/**
 * Android implementation of CodeParser factory.
 *
 * Note: Android cannot access jvmMain code directly, so we provide a simplified
 * regex-based implementation similar to iOS. For full TreeSitter functionality,
 * consider using server-side parsing.
 */
actual fun createCodeParser(): CodeParser {
    return AndroidCodeParser()
}

/**
 * Simplified CodeParser for Android platform.
 * Uses regex-based parsing to extract basic code structure information.
 */
private class AndroidCodeParser : CodeParser {

    override suspend fun parseNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        return when (language) {
            Language.JAVA, Language.KOTLIN -> parseOOPNodes(sourceCode, filePath, language)
            Language.JAVASCRIPT, Language.TYPESCRIPT -> parseJSNodes(sourceCode, filePath, language)
            Language.PYTHON -> parsePythonNodes(sourceCode, filePath, language)
            else -> emptyList()
        }
    }

    override suspend fun parseNodesAndRelationships(
        sourceCode: String,
        filePath: String,
        language: Language
    ): Pair<List<CodeNode>, List<CodeRelationship>> {
        val nodes = parseNodes(sourceCode, filePath, language)
        val relationships = buildRelationships(nodes)
        return Pair(nodes, relationships)
    }

    override suspend fun parseCodeGraph(
        files: Map<String, String>,
        language: Language
    ): CodeGraph {
        val allNodes = mutableListOf<CodeNode>()
        val allRelationships = mutableListOf<CodeRelationship>()

        for ((filePath, sourceCode) in files) {
            val (nodes, relationships) = parseNodesAndRelationships(sourceCode, filePath, language)
            allNodes.addAll(nodes)
            allRelationships.addAll(relationships)
        }

        return CodeGraph(
            nodes = allNodes,
            relationships = allRelationships,
            metadata = mapOf(
                "language" to language.name,
                "fileCount" to files.size.toString(),
                "platform" to "Android"
            )
        )
    }

    override suspend fun parseImports(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<ImportInfo> {
        return when (language) {
            Language.JAVA, Language.KOTLIN -> extractJvmImports(sourceCode, filePath)
            Language.PYTHON -> extractPythonImports(sourceCode, filePath)
            Language.JAVASCRIPT, Language.TYPESCRIPT -> extractJsImports(sourceCode, filePath)
            else -> emptyList()
        }
    }

    private fun extractJvmImports(content: String, filePath: String): List<ImportInfo> {
        val importRegex = Regex("""import\s+(static\s+)?([a-zA-Z_][\w.]*[\w*])""")
        return importRegex.findAll(content).mapIndexed { index, match ->
            ImportInfo(
                path = match.groupValues[2].removeSuffix(".*"),
                type = ImportType.MODULE,
                filePath = filePath,
                startLine = index,
                endLine = index
            )
        }.toList()
    }

    private fun extractPythonImports(content: String, filePath: String): List<ImportInfo> {
        val imports = mutableListOf<ImportInfo>()
        var lineIndex = 0

        val fromImportRegex = Regex("""from\s+([\w.]+)\s+import""")
        fromImportRegex.findAll(content).forEach { match ->
            imports.add(ImportInfo(
                path = match.groupValues[1],
                type = ImportType.MODULE,
                filePath = filePath,
                startLine = lineIndex++,
                endLine = lineIndex
            ))
        }

        val importRegex = Regex("""^import\s+([\w.]+)""", RegexOption.MULTILINE)
        importRegex.findAll(content).forEach { match ->
            imports.add(ImportInfo(
                path = match.groupValues[1],
                type = ImportType.MODULE,
                filePath = filePath,
                startLine = lineIndex++,
                endLine = lineIndex
            ))
        }

        return imports
    }

    private fun extractJsImports(content: String, filePath: String): List<ImportInfo> {
        val imports = mutableListOf<ImportInfo>()
        var lineIndex = 0

        val es6ImportRegex = Regex("""import\s+(?:.+\s+from\s+)?['"]([@\w./-]+)['"]""")
        es6ImportRegex.findAll(content).forEach { match ->
            imports.add(ImportInfo(
                path = match.groupValues[1],
                type = ImportType.MODULE,
                filePath = filePath,
                startLine = lineIndex++,
                endLine = lineIndex
            ))
        }

        return imports
    }

    private fun parseOOPNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val lines = sourceCode.lines()
        val packageName = extractPackageName(sourceCode)

        val classPattern = Regex("""(class|interface|enum|object)\s+(\w+)""")

        for ((index, line) in lines.withIndex()) {
            val currentLine = index + 1

            classPattern.find(line)?.let { match ->
                val type = when (match.groupValues[1]) {
                    "class", "object" -> CodeElementType.CLASS
                    "interface" -> CodeElementType.INTERFACE
                    "enum" -> CodeElementType.ENUM
                    else -> CodeElementType.CLASS
                }
                val name = match.groupValues[2]
                nodes.add(createCodeNode(name, type, packageName, filePath, currentLine, language))
            }
        }

        return nodes
    }

    private fun parseJSNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val lines = sourceCode.lines()

        val classPattern = Regex("""class\s+(\w+)""")

        for ((index, line) in lines.withIndex()) {
            val currentLine = index + 1

            classPattern.find(line)?.let { match ->
                val name = match.groupValues[1]
                nodes.add(createCodeNode(name, CodeElementType.CLASS, "", filePath, currentLine, language))
            }
        }

        return nodes
    }

    private fun parsePythonNodes(
        sourceCode: String,
        filePath: String,
        language: Language
    ): List<CodeNode> {
        val nodes = mutableListOf<CodeNode>()
        val lines = sourceCode.lines()

        val classPattern = Regex("""class\s+(\w+)""")

        for ((index, line) in lines.withIndex()) {
            val currentLine = index + 1

            classPattern.find(line)?.let { match ->
                val name = match.groupValues[1]
                nodes.add(createCodeNode(name, CodeElementType.CLASS, "", filePath, currentLine, language))
            }
        }

        return nodes
    }

    private fun extractPackageName(sourceCode: String): String {
        val packagePattern = Regex("""package\s+([\w.]+)""")
        return packagePattern.find(sourceCode)?.groupValues?.get(1) ?: ""
    }

    private fun createCodeNode(
        name: String,
        type: CodeElementType,
        packageName: String,
        filePath: String,
        startLine: Int,
        language: Language
    ): CodeNode {
        val qualifiedName = if (packageName.isNotEmpty()) "$packageName.$name" else name

        return CodeNode(
            id = qualifiedName.hashCode().toString(),
            type = type,
            name = name,
            packageName = packageName,
            filePath = filePath,
            startLine = startLine,
            endLine = startLine + 10,
            startColumn = 0,
            endColumn = 0,
            qualifiedName = qualifiedName,
            content = "",
            metadata = mapOf("language" to language.name, "platform" to "Android")
        )
    }

    private fun buildRelationships(nodes: List<CodeNode>): List<CodeRelationship> {
        // Simplified: no relationships for basic parsing
        return emptyList()
    }
}
