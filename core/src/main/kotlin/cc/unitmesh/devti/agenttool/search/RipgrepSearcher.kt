// Copyright 2024 Cline Bot Inc. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.agenttool.search

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.SystemIndependent
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

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
                val rgPath = findRipgrepBinary()
                if (rgPath == null) {
                    return@supplyAsync "Ripgrep binary not found, try install it first: https://github.com/BurntSushi/ripgrep?tab=readme-ov-file#installation"
                }

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

        // try get from /usr/local/bin/rg if macos
        if (osName.contains("mac")) {
            val path = Paths.get("/usr/local/bin/rg")
            if (path.toFile().exists()) {
                return path
            }
        }

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
            MutableList<RipgrepSearchResult> {
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

    private fun formatResults(results: MutableList<RipgrepSearchResult>, basePath: String): String {
        val output = StringBuilder()
        val grouped: MutableMap<String?, MutableList<RipgrepSearchResult?>?> =
            LinkedHashMap<String?, MutableList<RipgrepSearchResult?>?>()

        for (result in results) {
            val relPath = getRelativePath(basePath, result.filePath!!)
            grouped.computeIfAbsent(relPath) { k: String? -> ArrayList<RipgrepSearchResult?>() }!!.add(result)
        }

        for (entry in grouped.entries) {
            output.append("## filepath: ").append(entry.key).append("\n")
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
