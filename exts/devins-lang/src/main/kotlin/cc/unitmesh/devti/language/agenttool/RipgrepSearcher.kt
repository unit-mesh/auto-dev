// Copyright 2024 Cline Bot Inc. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.language.agenttool

import com.google.gson.JsonParser
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.SystemIndependent
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.text.compareTo
import kotlin.text.get


@Serializable
public data class SearchResult(
    var filePath: String? = null,
    var line: Int = 0,
    var column: Int = 0,
    var match: String? = null,
    var beforeContext: MutableList<String?> = ArrayList<String?>(),
    var afterContext: MutableList<String?> = ArrayList<String?>()
)

public class RipgrepOutputProcessor : ProcessAdapter() {
    private val results: MutableList<SearchResult> = ArrayList<SearchResult>()
    private var currentResult: SearchResult? = null

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        if (outputType === ProcessOutputTypes.STDOUT) {
            parseJsonLine(event.text)
        }
    }

    private val jsonBuffer = StringBuilder()

    fun parseJsonLine(line: String) {
        if (line.isBlank()) {
            return
        }

        jsonBuffer.append(line)

        // Try to parse the buffer as JSON
        val json = try {
            JsonParser.parseString(jsonBuffer.toString())
        } catch (e: Exception) {
            // If parsing fails, it might be because the JSON is incomplete
            // So we just return and wait for more lines
            return
        }

        // If parsing succeeds, clear the buffer and process the JSON
        jsonBuffer.clear()

        if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            val type = jsonObject.get("type").asString

            when (type) {
                "match" -> {
                    val data = jsonObject.getAsJsonObject("data")
                    val path = data.getAsJsonObject("path").get("text").asString
                    val lines = data.getAsJsonObject("lines").get("text").asString
                    val lineNumber = data.get("line_number").asInt
                    val absoluteOffset = data.get("absolute_offset").asInt
                    val submatches = data.getAsJsonArray("submatches")

                    currentResult = SearchResult(
                        filePath = path,
                        line = lineNumber,
                        column = absoluteOffset,
                        match = lines.trim()
                    )

                    submatches.forEach { submatch ->
                        val submatchObj = submatch.asJsonObject
                        val matchText = submatchObj.get("match").asJsonObject.get("text").asString
                        currentResult?.match = matchText
                    }

                    results.add(currentResult!!)
                }

                "context" -> {
                    val data = jsonObject.getAsJsonObject("data")
                    val lines = data.getAsJsonObject("lines").get("text").asString
                    val lineNumber = data.get("line_number").asInt

                    if (currentResult != null) {
                        if (lineNumber < currentResult!!.line) {
                            currentResult!!.beforeContext.add(lines.trim())
                        } else {
                            currentResult!!.afterContext.add(lines.trim())
                        }
                    }
                }
            }
        }
    }

    fun getResults(): MutableList<SearchResult> {
        if (currentResult != null) {
            results.add(currentResult!!)
        }

        return results
    }
}

/**
 * 使用Ripgrep进行文件搜索
 * Inspired by: https://github.com/cline/cline/blob/main/src/services/ripgrep/index.ts Apache-2.0
 */
object RipgrepSearcher {
    private val LOG = Logger.getInstance(RipgrepSearcher::class.java)

    fun searchFiles(
        project: Project,
        searchDirectory: String,
        regexPattern: String,
        filePattern: String?
    ): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync<String> {
            try {
                val rgPath = findRipgrepBinary() ?: throw IOException("Ripgrep binary not found")
                val results = executeRipgrep(
                    project,
                    rgPath,
                    searchDirectory,
                    regexPattern,
                    filePattern
                )
                return@supplyAsync formatResults(results, project.basePath!!)
            } catch (e: Exception) {
                LOG.error("Search failed", e)
                return@supplyAsync "Search error: " + e.message
            }
        }
    }

    @Throws(IOException::class)
    fun findRipgrepBinary(): Path? {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        val binName = if (osName.contains("win")) "rg.exe" else "rg"

        val pb = ProcessBuilder("which", binName)
        val process = pb.start()
        try {
            if (process.waitFor(1, TimeUnit.SECONDS) && process.exitValue() == 0) {
                val path = String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8).trim { it <= ' ' }
                return Paths.get(path)
            }
        } catch (_: InterruptedException) {
            return null
        }

        return null
    }

    @Throws(IOException::class)
    private fun executeRipgrep(project: Project, rgPath: Path, directory: String, regex: String, filePattern: String?):
            MutableList<SearchResult> {
        val cmd = getCommandLine(rgPath, regex, filePattern, directory, project.basePath)

        val handler: OSProcessHandler = ColoredProcessHandler(cmd)
        val processor = RipgrepOutputProcessor()
        handler.addProcessListener(processor)

        handler.startNotify()
        handler.waitFor()

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

        for (result in results) {
            val relPath = getRelativePath(basePath, result.filePath!!)
            grouped.computeIfAbsent(relPath) { k: String? -> ArrayList<SearchResult?>() }!!.add(result)
        }

        for (entry in grouped.entries) {
            output.append("### filepath: ").append(entry.key).append("\n")
            val filePath = Paths.get(basePath, entry.key)
            val content = filePath.toFile().readLines()

            val lineNumbers = entry.value!!.map { it!!.line }

            val displayLines = mutableSetOf<Int>()
            for (lineNumber in lineNumbers) {
                val start = 1.coerceAtLeast(lineNumber - 4)
                val end = content.size.coerceAtMost(lineNumber + 4)
                for (i in start..end) {
                    displayLines.add(i)
                }
            }

            val sortedDisplayLines = displayLines.sorted()
            for (lineNumber in sortedDisplayLines) {
                val line = content.getOrNull(lineNumber - 1)
                if (line != null) {
                    output.append(lineNumber).append(" ").append(line).append("\n")
                }
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
}
