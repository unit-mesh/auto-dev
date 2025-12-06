package cc.unitmesh.xuiper.eval

import cc.unitmesh.xuiper.ast.NanoNode
import cc.unitmesh.xuiper.dsl.NanoDSL
import cc.unitmesh.xuiper.ir.NanoIR
import cc.unitmesh.xuiper.parser.ParseResult
import java.io.File

/**
 * Validates NanoDSL files and reports parsing results.
 *
 * Run with: ./gradlew :xuiper-ui:validateDsl
 * Or with verbose: ./gradlew :xuiper-ui:validateDsl -Pverbose=true
 */
fun main(args: Array<String>) {
    val dir = args.firstOrNull() ?: "testcases/actual/integration"
    val verbose = args.getOrNull(1) == "verbose"
    val directory = File(dir)

    if (!directory.exists() || !directory.isDirectory) {
        println("❌ Directory not found: ${directory.absolutePath}")
        return
    }

    val files = directory.listFiles { f -> f.extension == "nanodsl" }?.sortedBy { it.name } ?: emptyList()

    if (files.isEmpty()) {
        println("⚠️  No .nanodsl files found in $dir")
        return
    }

    println("=" .repeat(70))
    println("NanoDSL Validation Report")
    println("=" .repeat(70))
    println("Directory: ${directory.absolutePath}")
    println("Files: ${files.size}")
    println("-".repeat(70))

    var passed = 0
    var failed = 0
    val errors = mutableListOf<Pair<String, String>>()

    files.forEach { file ->
        val source = file.readText()
        val result = NanoDSL.parseResult(source)

        when (result) {
            is ParseResult.Success -> {
                // Try to convert to IR to verify full pipeline
                try {
                    val ir = NanoDSL.toIR(result.ast)
                    val irStats = countIRNodes(ir)
                    val astStats = countASTNodes(result.ast)
                    println("✅ ${file.name}")
                    println("   AST: ${result.ast.name}, children=${result.ast.children.size}, total nodes=${astStats.totalNodes}")
                    println("   IR:  ${irStats.types.joinToString(", ")} (${irStats.totalNodes} nodes)")

                    if (verbose) {
                        println("   AST Tree:")
                        printASTTree(result.ast, "   ")
                    }
                    passed++
                } catch (e: Exception) {
                    println("⚠️  ${file.name} - Parsed but IR conversion failed")
                    println("   Error: ${e.message}")
                    errors.add(file.name to "IR conversion: ${e.message}")
                    failed++
                }
            }
            is ParseResult.Failure -> {
                println("❌ ${file.name}")
                result.errors.forEach { error ->
                    println("   Line ${error.line}: ${error.message}")
                }
                errors.add(file.name to result.errors.joinToString("; ") { it.message })
                failed++
            }
        }
        println()
    }

    println("=".repeat(70))
    println("Summary: $passed passed, $failed failed (${files.size} total)")
    println("=".repeat(70))

    if (errors.isNotEmpty()) {
        println("\nErrors:")
        errors.forEach { (name, msg) ->
            println("  - $name: $msg")
        }
    }
}

private fun countASTNodes(node: NanoNode): IRStats {
    val types = mutableSetOf<String>()
    var count = 0

    fun traverse(n: NanoNode) {
        types.add(n::class.simpleName ?: "Unknown")
        count++
        when (n) {
            is NanoNode.Component -> n.children.forEach { traverse(it) }
            is NanoNode.VStack -> n.children.forEach { traverse(it) }
            is NanoNode.HStack -> n.children.forEach { traverse(it) }
            is NanoNode.Card -> n.children.forEach { traverse(it) }
            is NanoNode.Conditional -> {
                n.thenBranch.forEach { traverse(it) }
                n.elseBranch?.forEach { traverse(it) }
            }
            is NanoNode.ForLoop -> n.body.forEach { traverse(it) }
            else -> {}
        }
    }

    traverse(node)
    return IRStats(types, count)
}

private fun printASTTree(node: NanoNode, indent: String) {
    when (node) {
        is NanoNode.Component -> {
            println("${indent}Component(${node.name}) [${node.children.size} children]")
            node.children.forEach { printASTTree(it, "$indent  ") }
        }
        is NanoNode.VStack -> {
            println("${indent}VStack [${node.children.size} children]")
            node.children.forEach { printASTTree(it, "$indent  ") }
        }
        is NanoNode.HStack -> {
            println("${indent}HStack [${node.children.size} children]")
            node.children.forEach { printASTTree(it, "$indent  ") }
        }
        is NanoNode.Card -> {
            println("${indent}Card [${node.children.size} children]")
            node.children.forEach { printASTTree(it, "$indent  ") }
        }
        is NanoNode.Text -> println("${indent}Text(\"${node.content.take(30)}...\")")
        is NanoNode.Button -> println("${indent}Button(\"${node.label}\")")
        is NanoNode.Conditional -> {
            println("${indent}Conditional(${node.condition}) [then=${node.thenBranch.size}, else=${node.elseBranch?.size ?: 0}]")
            node.thenBranch.forEach { printASTTree(it, "$indent  ") }
            node.elseBranch?.forEach { printASTTree(it, "$indent  [else] ") }
        }
        is NanoNode.ForLoop -> {
            println("${indent}ForLoop(${node.variable} in ${node.iterable}) [${node.body.size} children]")
            node.body.forEach { printASTTree(it, "$indent  ") }
        }
        else -> println("${indent}${node::class.simpleName}")
    }
}

private data class IRStats(
    val types: Set<String>,
    val totalNodes: Int
)

private fun countIRNodes(ir: NanoIR): IRStats {
    val types = mutableSetOf<String>()
    var count = 0
    
    fun traverse(node: NanoIR) {
        types.add(node.type)
        count++
        node.children?.forEach { traverse(it) }
    }
    
    traverse(ir)
    return IRStats(types, count)
}

