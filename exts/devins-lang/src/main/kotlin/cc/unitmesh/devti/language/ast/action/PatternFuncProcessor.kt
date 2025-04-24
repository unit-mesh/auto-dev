package cc.unitmesh.devti.language.ast.action

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.ast.FrontMatterType
import cc.unitmesh.devti.language.ast.FunctionStatementProcessor
import cc.unitmesh.devti.language.ast.HobbitHole
import cc.unitmesh.devti.language.ast.Statement
import cc.unitmesh.devti.language.processor.ApprovalExecuteProcessor
import cc.unitmesh.devti.language.processor.BatchProcessor
import cc.unitmesh.devti.language.processor.CrawlProcessor
import cc.unitmesh.devti.language.processor.ExecuteProcessor
import cc.unitmesh.devti.language.processor.JsonPathProcessor
import cc.unitmesh.devti.language.processor.ThreadProcessor
import cc.unitmesh.devti.provider.toolchain.ToolchainFunctionProvider
import cc.unitmesh.devti.util.workerThread
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import cc.unitmesh.devti.language.processor.CaptureProcessor
import cc.unitmesh.devti.language.processor.ForeignFunctionProcessor
import cc.unitmesh.devti.util.findFile
import cc.unitmesh.devti.util.readText
import com.intellij.execution.ui.ConsoleViewContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.text.contains

open class PatternFuncProcessor(open val myProject: Project, open val hole: HobbitHole) {
    private val logger = logger<PatternActionProcessor>()

    /**
     * This function `patternFunctionExecute` is used to execute a specific action based on the type of `PatternActionFunc` provided.
     * It takes three parameters: `action`, `input`, and `lastResult`.
     *
     * @param action This is an instance of `PatternActionFunc` which is a sealed class. The function behavior changes based on the type of `PatternActionFunc`.
     * @param input This is a generic parameter which can be of any type. It is used in the `PatternActionFunc.Cat` case.
     * @param lastResult This is a generic parameter which can be of any type. It is used in all cases except `PatternActionFunc.Prompt`, `PatternActionFunc.Cat`, `PatternActionFunc.`Print`` and `PatternActionFunc.Xargs`.
     *
     * @return The return type is `Any`. The actual return type depends on the type of `PatternActionFunc`. For example, if `PatternActionFunc` is `Prompt`, it returns a `String`. If `PatternActionFunc` is `Grep`, it returns a `String` joined by "\n" from an `Array` or `String` that contains the specified patterns. If `PatternActionFunc` is `Sed`, it returns a `String` joined by "\n" from an `Array` or `String` where the specified pattern has been replaced. If `PatternActionFunc` is `Sort`, it returns a sorted `String` joined by "\n" from an `Array` or `String`. If `PatternActionFunc` is `Uniq`, it returns a `String` joined by "\n" from an `Array` or `String` with distinct elements. If `PatternActionFunc` is `Head`, it returns a `String` joined by "\n" from the first 'n' elements of an `Array` or `String`. If `PatternActionFunc` is `Tail`, it returns a `String` joined by "\n" from the last 'n' elements of an `Array` or `String`. If `PatternActionFunc` is `Cat`, it executes the `executeCatFunc` function. If `PatternActionFunc` is `Print`, it returns a `String` joined by "\n" from the `texts` property of `Print`. If `PatternActionFunc` is `Xargs`, it returns the `variables` property of `Xargs`. If `PatternActionFunc` is `UserCustom`, it logs an error message. If `PatternActionFunc` is of an unknown type, it logs an error message and returns an empty `String`.
     */
    open suspend fun patternFunctionExecute(
        action: PatternActionFunc,
        lastResult: Any,
        input: Any,
        variableTable: MutableMap<String, Any?> = mutableMapOf(),
    ): Any {
        return when (action) {
            is PatternActionFunc.Find -> {
                when (lastResult) {
                    is Array<*> -> {
                        (lastResult as Array<String>)
                            .filter { line -> line.contains(action.text) }
                            .toTypedArray()
                    }

                    else -> {
                        (lastResult as String).split("\n")
                            .filter { line -> line.contains(action.text) }
                            .joinToString("\n")
                    }
                }
            }

            is PatternActionFunc.Grep -> {
                val regexs = action.patterns.map { it.toRegex() }
                when (lastResult) {
                    is Array<*> -> {
                        val inputArray = (lastResult as Array<String>)
                        val result = regexs.map { regex ->
                            inputArray.map { line ->
                                regex.findAll(line)
                                    .map {
                                        if (it.groupValues.size > 1) {
                                            it.groupValues[1]
                                        } else {
                                            it.groupValues[0]
                                        }
                                    }.toList()
                            }.flatten()
                        }.flatten()

                        result.toTypedArray()
                    }

                    is String -> {
                        val result = regexs.map { regex ->
                            regex.findAll(lastResult)
                                .map {
                                    if (it.groupValues.size > 1) {
                                        it.groupValues[1]
                                    } else {
                                        it.groupValues[0]
                                    }
                                }.toList()
                        }.flatten().joinToString("\n")

                        result
                    }

                    else -> {
                        logger.error("Unknown pattern input for ${action.funcName}, lastResult: $lastResult")
                        ""
                    }
                }
            }

            is PatternActionFunc.Sed -> {
                when (lastResult) {
                    is Array<*> -> {
                        (lastResult as Array<String>).joinToString("\n") { line ->
                            line.replace(
                                action.pattern.toRegex(),
                                action.replacements
                            )
                        }
                    }

                    else -> {
                        (lastResult as String).split("\n").joinToString("\n") { line ->
                            line.replace(
                                action.pattern.toRegex(),
                                action.replacements
                            )
                        }
                    }
                }
            }

            is PatternActionFunc.Sort -> {
                when (lastResult) {
                    is Array<*> -> {
                        (lastResult as Array<String>).sorted()
                    }

                    else -> {
                        (lastResult as String).split("\n").sorted().joinToString("\n")
                    }
                }
            }

            is PatternActionFunc.Uniq -> {
                when (lastResult) {
                    is Array<*> -> {
                        (lastResult as Array<String>).distinct()
                    }

                    else -> {
                        (lastResult as String).split("\n").distinct().joinToString("\n")
                    }
                }
            }

            is PatternActionFunc.Head -> {
                when (lastResult) {
                    is Array<*> -> {
                        (lastResult as Array<String>).take(action.number.toInt())
                    }

                    else -> {
                        (lastResult as String).split("\n").take(action.number.toInt()).joinToString("\n")
                    }
                }
            }

            is PatternActionFunc.Tail -> {
                when (lastResult) {
                    is Array<*> -> {
                        (lastResult as Array<String>).takeLast(action.number.toInt())
                    }

                    else -> {
                        (lastResult as String).split("\n").takeLast(action.number.toInt()).joinToString("\n")
                    }
                }
            }

            is PatternActionFunc.Cat -> {
                val path: Array<String> = action.paths.map { it.fillVariable(variableTable) }.toTypedArray()
                cat(path, lastResult)
            }

            is PatternActionFunc.Print -> {
                if (action.texts.isEmpty()) {
                    return when (lastResult) {
                        is Array<*> -> {
                            (lastResult as Array<String>).joinToString("\n")
                        }

                        is List<*> -> {
                            (lastResult as List<String>).joinToString("\n")
                        }

                        else -> {
                            lastResult.toString()
                        }
                    }
                }

                action.texts.joinToString("\n") { it.fillVariable(variableTable) }
            }

            is PatternActionFunc.Xargs -> {
                action.variables
            }

            is PatternActionFunc.ToolchainFunction -> {
                /// maybe User custom functions
                val args: MutableList<Any> = action.args.toMutableList()
                /// add lastResult at args first
                when (lastResult) {
                    is String -> {
                        args.add(lastResult.fillVariable(variableTable))
                    }

                    is List<*> -> {
                        if (lastResult.isNotEmpty()) {
                            args.add(lastResult)
                        }
                    }

                    is Array<*> -> {
                        if (lastResult.isNotEmpty()) {
                            args.add(lastResult)
                        }
                    }

                    else -> {
                        args.add(lastResult)
                    }
                }

                val result = args.map {
                    when (it) {
                        is String -> it.fillVariable(variableTable)
                        else -> it
                    }
                }

                if (hole.foreignFunctions.containsKey(action.funcName)) {
                    val func = hole.foreignFunctions[action.funcName]!!
                    return ForeignFunctionProcessor.execute(myProject, action.funcName, result, variableTable, func)
                }

                ToolchainFunctionProvider.provide(myProject, action.funcName)
                    ?.execute(myProject, action.funcName, result, variableTable, commandName = action.funcName)
                    ?: logger.error(AutoDevBundle.message("shire.toolchain.function.not.found", action.funcName))
            }

            is PatternActionFunc.Notify -> {
                // action.message is empty get lastResult
                val message = action.message.ifEmpty {
                    lastResult.toString()
                }

                AutoDevNotifications.warn(myProject, message)
                // return last result for next step
                lastResult
            }

            is PatternActionFunc.From,
            is PatternActionFunc.Select,
            is PatternActionFunc.Where,
                -> {
                logger.error("Unknown pattern processor type: ${action.funcName}")
            }

            is PatternActionFunc.CaseMatch -> {
                val actions = evaluateCase(action, input) ?: return ""
                FunctionStatementProcessor(myProject, hole)
                    .execute(actions.value as Statement, mutableMapOf("output" to parseInput(input)))
                    .toString()
            }

            is PatternActionFunc.Crawl -> {
                val urls: MutableList<String> = mutableListOf()
                if (action.urls.isEmpty()) {
                    when (lastResult) {
                        is ArrayList<*> -> {
                            (lastResult as ArrayList<String>).forEach {
                                urls.add(it)
                            }
                        }

                        is String -> {
                            lastResult.split("\n").forEach {
                                urls.add(it)
                            }
                        }

                        else -> {
                            logger.warn("crawl error: $lastResult")
                        }
                    }
                } else {
                    urls.addAll(action.urls)
                }

                val finalUrls = urls.map { it.trim() }.filter { it.isNotEmpty() }
                CrawlProcessor.execute(finalUrls.toTypedArray())
            }

            is PatternActionFunc.Capture -> {
                CaptureProcessor.execute(myProject, action.fileName, action.nodeType)
            }

            is PatternActionFunc.Execute -> {
                /// don't need to fill variable for filename
                val variableNames: Array<String> = action.variableNames.map {
                    if (it.startsWith("\$")) {
                        it.substring(1)
                    } else {
                        it
                    }
                }.toTypedArray()

                ExecuteProcessor.execute(myProject, action.filename, variableNames, variableTable)
            }

            is PatternActionFunc.ApprovalExecute -> {
                val variableNames: Array<String> = action.variableNames.map {
                    if (it.startsWith("\$")) {
                        it.substring(1)
                    } else {
                        it
                    }
                }.toTypedArray()

                ApprovalExecuteProcessor.execute(myProject, action.filename, variableNames, variableTable,
                    approve = {
                        CoroutineScope(workerThread).launch {
                            ExecuteProcessor.execute(myProject, action.filename, variableNames, variableTable)
                        }
                    })
            }

            is PatternActionFunc.Batch -> {
                val inputs: List<String> = action.inputs.map { input ->
                    if (input.startsWith("\$")) {
                        when (val variable = variableTable[input.substring(1)]) {
                            is String -> listOf(variable.toString())
                            is List<*> -> variable.map(Any?::toString)
                            is Array<*> -> variable.map(Any?::toString)
                            else -> listOf()
                        }
                    } else {
                        listOf(input)
                    }
                }.flatten()

                BatchProcessor.execute(myProject, action.fileName, inputs, action.batchSize, variableTable)
            }

            is PatternActionFunc.Thread -> {
                val varNames = action.variableNames.toMutableList().apply {
                    if (!contains("output")) {
                        add("output")
                    }
                }.map {
                    it.fillVariable(variableTable)
                }.toTypedArray()

                if (!variableTable.containsKey("output")) {
                    variableTable["output"] = lastResult
                }

                ThreadProcessor.execute(myProject, action.fileName, varNames, variableTable)
            }

            is PatternActionFunc.JsonPath -> {
                var jsonStr = action.obj ?: lastResult as String
                jsonStr = jsonStr.fillVariable(variableTable)

                JsonPathProcessor.execute(myProject, jsonStr, action) ?: jsonStr
            }

            is PatternActionFunc.Destroy -> {
                TODO()
            }
            is PatternActionFunc.LineNo -> {
                when (lastResult) {
                    is Array<*> -> {
                        (lastResult as Array<String>).mapIndexed { index, line ->
                            "${index + 1}: $line"
                        }.toTypedArray()
                    }

                    else -> {
                        (lastResult as String).split("\n").mapIndexed { index, line ->
                            "${index + 1}: $line"
                        }.joinToString("\n")
                    }
                }
            }
        }
    }

    private fun evaluateCase(action: PatternActionFunc.CaseMatch, input: Any): FrontMatterType.EXPRESSION? {
        var fitCondition = action.keyValue.firstOrNull { it.key.toValue() == parseInput(input) }
        if (fitCondition == null) {
            fitCondition = action.keyValue.firstOrNull { it.key.toValue() == "default" }
        }

        return fitCondition?.value
    }

    private fun parseInput(input: Any): String {
        return when (input) {
            is String -> {
                input
            }

            is Array<*> -> {
                input.firstOrNull().toString()
            }

            else -> {
                input.toString()
            }
        }
    }

    fun cat(paths: Array<String>, input: Any): String {
        val absolutePaths: List<VirtualFile> = resolvePaths(paths, input)
        return absolutePaths.joinToString("\n") { it.readText() }
    }

    /**
     * @param userPaths The paths provided by the user in the script: `cat("file1.txt", "file2.txt")`.
     * @param patterMatchPaths The paths provided by the pattern match: `/.*.txt/ { cat } `.
     */
    private fun resolvePaths(userPaths: Array<out String>, patterMatchPaths: Any): List<VirtualFile> {
        val baseDir = myProject.guessProjectDir()!!
        var paths = userPaths
        if (userPaths.isEmpty()) {
            when (patterMatchPaths) {
                is List<*> -> {
                    paths = (patterMatchPaths as List<String>).toTypedArray()
                }

                is Array<*> -> {
                    paths = patterMatchPaths as Array<String>
                }

                is String -> {
                    paths = arrayOf(patterMatchPaths)
                }

                else -> {
                    logger.warn("resolvePaths error: $patterMatchPaths")
                }
            }
        }

        val absolutePaths: List<VirtualFile> = paths.mapNotNull {
            baseDir.findFile(it) ?: try {
                LocalFileSystem.getInstance().findFileByIoFile(File(it))
            } catch (e: Exception) {
                null
            }
        }

        return absolutePaths
    }
}

fun String.fillVariable(
    variableTable: MutableMap<String, Any?>,
): String {
    return if (this.startsWith("\$")) {
        variableTable[this.substring(1)]?.toString() ?: this
    } else {
        this
    }
}
