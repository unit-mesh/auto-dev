package cc.unitmesh.devti.language.ast.action

import cc.unitmesh.devti.language.ast.CaseKeyValue
import cc.unitmesh.devti.language.ast.Statement
import cc.unitmesh.devti.language.ast.VariableElement
import com.intellij.openapi.diagnostic.logger

/**
 * The `PatternActionFunc` is a sealed class in Kotlin that represents a variety of pattern action functions.
 * Each subclass represents a different function, and each has a unique set of properties relevant to its function.
 *
 * @property funcName The name of the function.
 */
sealed class PatternActionFunc(val type: PatternActionFuncDef) {
    open val funcName: String = type.funcName

    /**
     * Grep subclass for searching with one or more patterns.
     *
     * @property patterns The patterns to search for.
     */
    class Grep(vararg val patterns: String) : PatternActionFunc(PatternActionFuncDef.GREP)

    /**
     * Find subclass for searching with text
     * @property text The text to search for.
     */
    class Find(val text: String) : PatternActionFunc(PatternActionFuncDef.FIND)

    /**
     * Sed subclass for find and replace operations.
     *
     * @property pattern The pattern to search for.
     * @property replacements The string to replace matches with.
     *
     * For example, `sed("foo", "bar")` would replace all instances of "foo" with "bar".
     */
    class Sed(val pattern: String, val replacements: String, val isRegex: Boolean = true) :
        PatternActionFunc(PatternActionFuncDef.SED)

    /**
     * Sort subclass for sorting with one or more arguments.
     *
     * @property arguments The arguments to use for sorting.
     */
    class Sort(vararg val arguments: String) : PatternActionFunc(PatternActionFuncDef.SORT)

    /**
     * Uniq subclass for removing duplicates based on one or more arguments.
     *
     * @property texts The texts to process for uniqueness.
     */
    class Uniq(vararg val texts: String) : PatternActionFunc(PatternActionFuncDef.UNIQ)

    /**
     * Head subclass for retrieving the first few lines.
     *
     * @property number The number of lines to retrieve from the start.
     */
    class Head(val number: Number) : PatternActionFunc(PatternActionFuncDef.HEAD)

    /**
     * Tail subclass for retrieving the last few lines.
     *
     * @property number The number of lines to retrieve from the end.
     */
    class Tail(val number: Number) : PatternActionFunc(PatternActionFuncDef.TAIL)

    /**
     * Xargs subclass for processing one or more variables.
     *
     * @property variables The variables to process.
     */
    class Xargs(vararg val variables: String) : PatternActionFunc(PatternActionFuncDef.XARGS)

    /**
     * Print subclass for printing one or more texts.
     *
     * @property texts The texts to be printed.
     */
    class Print(vararg val texts: String) : PatternActionFunc(PatternActionFuncDef.PRINT)

    /**
     * Cat subclass for concatenating one or more files.
     * Paths can be absolute or relative to the current working directory.
     */
    class Cat(vararg val paths: String) : PatternActionFunc(PatternActionFuncDef.CAT)

    /**
     * Select subclass for selecting one or more elements.
     */
    class From(val variables: List<VariableElement>) : PatternActionFunc(PatternActionFuncDef.FROM)

    /**
     * Where subclass for filtering elements.
     */
    class Where(val statement: Statement) : PatternActionFunc(PatternActionFuncDef.WHERE)

    /**
     * OrderBy subclass for ordering elements.
     */
    class Select(val statements: List<Statement>) : PatternActionFunc(PatternActionFuncDef.SELECT)

    /**
     * Execute a shire script
     */
    class Execute(val filename: String, val variableNames: Array<String>) :
        PatternActionFunc(PatternActionFuncDef.EXECUTE)

    /**
     * Approval Execution
     */
    class ApprovalExecute(val filename: String, val variableNames: Array<String>) :
        PatternActionFunc(PatternActionFuncDef.APPROVAL_EXECUTE)

    /**
     * Use IDE Notify
     */
    class Notify(val message: String) : PatternActionFunc(PatternActionFuncDef.NOTIFY)

    /**
     * Case Match
     */
    class CaseMatch(val keyValue: List<CaseKeyValue>) : PatternActionFunc(PatternActionFuncDef.CASE_MATCH)

    /**
     * The Crawl function is used to crawl a list of urls, get markdown from html and save it to a file.
     *
     * @param urls The urls to crawl.
     */
    class Crawl(vararg val urls: String) : PatternActionFunc(PatternActionFuncDef.CRAWL)

    /**
     * The capture function used to capture file by NodeType
     *
     * @param fileName The file name to save the capture to.
     * @param nodeType The node type to capture.
     */
    class Capture(val fileName: String, val nodeType: String) : PatternActionFunc(PatternActionFuncDef.CAPTURE)

    /**
     * The thread function will run the function in a new thread
     *
     * @param fileName The file name to run
     */
    class Thread(val fileName: String, val variableNames: Array<String>) :
        PatternActionFunc(PatternActionFuncDef.THREAD)

    /**
     * The jsonpath function will parse the json and get the value by jsonpath
     */
    class JsonPath(val obj: String?, val path: String, val sseMode: Boolean = false) :
        PatternActionFunc(PatternActionFuncDef.JSONPATH)

    class Destroy : PatternActionFunc(PatternActionFuncDef.DESTROY)

    class Batch(val fileName: String, val inputs: List<String>, val batchSize: Int = 1) :
        PatternActionFunc(PatternActionFuncDef.BATCH)

    /**
     * Line Number
     */
    class LineNo(var text: String) : PatternActionFunc(PatternActionFuncDef.LINE_NO)

    /**
     * User Custom Functions
     */
    class ToolchainFunction(override val funcName: String, val args: List<String>) :
        PatternActionFunc(PatternActionFuncDef.TOOLCHAIN_FUNCTION) {
        override fun toString(): String {
            return "$funcName(${args.joinToString(", ")})"
        }
    }

    companion object {
        private val logger = logger<PatternActionFunc>()

        fun findDocByName(funcName: String?): String? {
            val actionFuncType = PatternActionFuncDef.entries.find { it.funcName == funcName } ?: return null
            return """
                | ${actionFuncType.description}
                | 
                | Example:
                | ${actionFuncType.example}
            """.trimMargin()
        }

        fun all(): List<PatternActionFuncDef> {
            return PatternActionFuncDef.entries
        }

        fun from(funcName: String, args: List<String>): PatternActionFunc? {
            return when (PatternActionFuncDef.entries.find { it.funcName == funcName }) {
                PatternActionFuncDef.GREP -> {
                    if (args.isEmpty()) {
                        logger.error("PatternActionFun,`grep` func requires at least 1 argument")
                        return null
                    }
                    Grep(*args.toTypedArray())
                }

                PatternActionFuncDef.SORT -> Sort(*args.toTypedArray())

                PatternActionFuncDef.FIND -> {
                    if (args.isEmpty()) {
                        logger.error("PatternActionFun,`find` func requires at least 1 argument")
                        return null
                    }
                    Find(args[0])
                }

                PatternActionFuncDef.SED -> {
                    if (args.size < 2) {
                        logger.error("PatternActionFun,`sed` func requires at least 2 arguments")
                        return null
                    }
                    if (args[0].startsWith("/") && args[0].endsWith("/")) {
                        Sed(args[0], args[1], true)
                    } else {
                        Sed(args[0], args[1])
                    }
                }

                PatternActionFuncDef.XARGS -> Xargs(*args.toTypedArray())

                PatternActionFuncDef.UNIQ -> Uniq(*args.toTypedArray())

                PatternActionFuncDef.HEAD -> {
                    if (args.isEmpty()) {
                        Head(10)
                    } else {
                        Head(args[0].toInt())
                    }
                }

                PatternActionFuncDef.TAIL -> {
                    if (args.isEmpty()) {
                        Tail(10)
                    } else {
                        Tail(args[0].toInt())
                    }
                }

                PatternActionFuncDef.PRINT -> Print(*args.toTypedArray())

                PatternActionFuncDef.CAT -> Cat(*args.toTypedArray())

                PatternActionFuncDef.EXECUTE -> {
                    val first = args.firstOrNull() ?: ""
                    val rest = args.drop(1).toTypedArray()
                    Execute(first, rest)
                }

                PatternActionFuncDef.APPROVAL_EXECUTE -> {
                    val first = args.firstOrNull() ?: ""
                    val rest = args.drop(1).toTypedArray()
                    ApprovalExecute(first, rest)
                }

                PatternActionFuncDef.NOTIFY -> {
                    val first = args.firstOrNull() ?: ""
                    Notify(first)
                }
                PatternActionFuncDef.CRAWL -> {
                    val urls: List<String> = args.filter { it.trim().isNotEmpty() }
                    Crawl(*urls.toTypedArray())
                }

                PatternActionFuncDef.CAPTURE -> {
                    if (args.size < 2) {
                        logger.error("PatternActionFun,`capture` func requires at least 2 arguments")
                        return null
                    }
                    Capture(args[0], args[1])
                }

                PatternActionFuncDef.THREAD -> {
                    if (args.isEmpty()) {
                        logger.error("PatternActionFun,`thread` func requires at least 1 argument")
                        return null
                    }
                    val rest = args.drop(1).toTypedArray()
                    Thread(args.first(), rest)
                }

                PatternActionFuncDef.JSONPATH -> {
                    if (args.isEmpty()) {
                        logger.error("PatternActionFun,`jsonpath` func requires at least 1 argument")
                        return null
                    }
                    if (args.size < 2) {
                        JsonPath(null, args[0], false)
                    } else {
                        when (args[1]) {
                            "true" -> JsonPath(null, args[0], true)
                            else -> JsonPath(args[0], args[1])
                        }
                    }
                }

                PatternActionFuncDef.FROM,
                PatternActionFuncDef.WHERE,
                PatternActionFuncDef.SELECT,
                PatternActionFuncDef.CASE_MATCH,
                    -> {
                    ToolchainFunction(funcName, args)
                }

                PatternActionFuncDef.BATCH -> {
                    Batch(args[0], args.drop(1))
                }

                PatternActionFuncDef.DESTROY -> {
                    Destroy()
                }

                PatternActionFuncDef.TOOLCHAIN_FUNCTION -> ToolchainFunction(funcName, args)
                PatternActionFuncDef.LINE_NO -> {
                    if (args.isEmpty()) {
                        logger.error("PatternActionFun,`lineNo` func requires at least 1 argument")
                        return null
                    }
                    LineNo(args[0])
                }

                else -> {
                    ToolchainFunction(funcName, args)
                }
            }
        }
    }
}
