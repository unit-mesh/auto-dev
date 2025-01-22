// Copyright 2024 Cline Bot Inc. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.language.compiler.exec

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.SystemIndependent
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.math.min


/**
 * 使用Ripgrep进行文件搜索
 * Inspired by: https://github.com/cline/cline/blob/main/src/services/ripgrep/index.ts Apache-2.0
 */
object RipgrepSearcher {
    private val LOG = Logger.getInstance(RipgrepSearcher::class.java)
    private const val MAX_RESULTS = 300

    fun searchFiles(
        project: Project,
        searchDirectory: String,
        regexPattern: String,
        filePattern: String?
    ): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync<String?> {
            try {
                val rgPath = findRipgrepBinary()
                val results = executeRipgrep(
                    project,
                    rgPath,
                    searchDirectory,
                    regexPattern,
                    filePattern ?: "*"
                )
                return@supplyAsync formatResults(results, project.basePath!!)
            } catch (e: Exception) {
                LOG.error("Search failed", e)
                return@supplyAsync "Search error: " + e.message
            }
        }
    }

    @Throws(IOException::class)
    fun findRipgrepBinary(): Path {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        val binName = if (osName.contains("win")) "rg.exe" else "rg"

        // 尝试系统PATH查找
        val pb = ProcessBuilder("which", binName)
        val process = pb.start()
        try {
            if (process.waitFor(1, TimeUnit.SECONDS) && process.exitValue() == 0) {
                val path = String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8).trim { it <= ' ' }
                return Paths.get(path)
            }
        } catch (ignored: InterruptedException) {
            throw IOException("Ripgrep binary not found")
        }

        throw IOException("Ripgrep binary not found")
    }

    @Throws(IOException::class)
    private fun executeRipgrep(
        project: Project,
        rgPath: Path,
        directory: String,
        regex: String,
        filePattern: String
    ): MutableList<SearchResult> {
        val cmd = getCommandLine(rgPath, regex, filePattern, directory, project.basePath)

        val handler: OSProcessHandler = ColoredProcessHandler(cmd)
        val processor = RipgrepOutputProcessor()
        handler.addProcessListener(processor)

        ProgressManager.getInstance().runProcessWithProgressSynchronously(Runnable {
            try {
                handler.startNotify()
                handler.waitFor()
            } catch (e: RuntimeException) {
                throw IOException("Process execution failed", e)
            }
        }, "Searching with ripgrep...", false, project)

        return processor.getResults()
    }

    fun getCommandLine(
        rgPath: Path,
        regex: String? = null,
        filePattern: String? = null,
        directory: String? = null,
        basePath: @SystemIndependent @NonNls String? = null,
    ): GeneralCommandLine {
        val cmd = GeneralCommandLine(rgPath.toString())
        cmd.withWorkDirectory(basePath)

        cmd.addParameters("--json")

        if (regex != null) {
            cmd.addParameters("-e", regex)
        }

        if (filePattern != null) {
            cmd.addParameters("--glob", filePattern)
        }

        cmd.addParameters("--context", "1")

        if (directory != null) {
            cmd.addParameters(directory)
        }

        cmd.charset = StandardCharsets.UTF_8
        return cmd
    }

    private fun formatResults(results: MutableList<SearchResult>, basePath: String): String {
        val output = StringBuilder()
        val grouped: MutableMap<String?, MutableList<SearchResult?>?> =
            LinkedHashMap<String?, MutableList<SearchResult?>?>()

        // 分组结果
        for (result in results) {
            val relPath = getRelativePath(basePath, result.filePath!!)
            grouped.computeIfAbsent(relPath) { k: String? -> ArrayList<SearchResult?>() }!!.add(result)
        }

        // 构建输出
        for (entry in grouped.entries) {
            output.append(entry.key).append("\n│----\n")
            for (result in entry.value!!) {
                val context: MutableList<String?> = ArrayList<String?>()
                context.addAll(result!!.beforeContext)
                context.add(result.match)
                context.addAll(result.afterContext)

                context.forEach(Consumer { line: String? ->
                    output.append("│").append(line!!.trim { it <= ' ' }).append("\n")
                })
                output.append("│----\n")
            }

            output.append("\n")
        }

        return output.toString()
    }

    private fun getRelativePath(basePath: String, absolutePath: String): String {
        val base = Paths.get(basePath)
        val target = Paths.get(absolutePath)
        return base.relativize(target).toString().replace('\\', '/')
    }

    class SearchResult {
        var filePath: String? = null
        var line: Int = 0
        var column: Int = 0
        var match: String? = null
        var beforeContext: MutableList<String?> = ArrayList<String?>()
        var afterContext: MutableList<String?> = ArrayList<String?>()
    }

    private class RipgrepOutputProcessor : ProcessAdapter() {
        private val results: MutableList<SearchResult> = ArrayList<SearchResult>()
        private var currentResult: SearchResult? = null

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            if (outputType === ProcessOutputTypes.STDOUT) {
                parseLine(event.text)
            }
        }

        fun parseLine(line: String) {
            try {
                val data = parseJsonLine(line)
                if (data == null) return

                val type = data.get("type") as String?
                if ("match" == type) {
                    if (currentResult != null) {
                        results.add(currentResult!!)
                    }
                    currentResult = SearchResult()
                    val matchData = data["data"] as MutableMap<*, *>
                    currentResult!!.filePath = (matchData["path"] as MutableMap<*, *>)["text"] as String
                    currentResult!!.line = (matchData["line_number"] as Number).toInt()
                    currentResult!!.match = (matchData["lines"] as MutableMap<*, *>)["text"].toString()
                } else if ("context" == type && currentResult != null) {
                    val ctxData = data["data"] as MutableMap<*, *>
                    val lineNumber = (ctxData["line_number"] as Number).toInt()
                    val text: String? = (ctxData["lines"] as MutableMap<*, *>)["text"].toString()

                    if (lineNumber < currentResult!!.line) {
                        currentResult!!.beforeContext.add(text)
                    } else {
                        currentResult!!.afterContext.add(text)
                    }
                }
            } catch (e: Exception) {
                LOG.warn("Error parsing rg output: " + line, e)
            }
        }

        fun parseJsonLine(line: String): MutableMap<String?, Any?>? {
            // 实现简化的JSON解析（实际应使用JSON库）
            if (!line.startsWith("{") || !line.endsWith("}")) return null
            // ... 解析逻辑 ...
            return mutableMapOf<String?, Any?>()
        }

        fun getResults(): MutableList<SearchResult> {
            if (currentResult != null) {
                results.add(currentResult!!)
            }

            return results.subList(0, min(results.size.toDouble(), MAX_RESULTS.toDouble()).toInt())
        }
    }
}