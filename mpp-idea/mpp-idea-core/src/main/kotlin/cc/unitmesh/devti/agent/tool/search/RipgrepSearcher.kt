// Copyright 2024 Cline Bot Inc. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.devti.agent.tool.search

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

        LOG.debug("Searching for ripgrep binary on OS: $osName")

        val result = when {
            osName.contains("win") -> findRipgrepBinaryOnWindows(binName)
            else -> findRipgrepBinaryOnUnix(binName)
        }

        if (result != null) {
            LOG.debug("Found ripgrep binary at: $result")
        } else {
            LOG.warn("Ripgrep binary not found. Please install ripgrep: https://github.com/BurntSushi/ripgrep#installation")
        }

        return result
    }

    private fun findRipgrepBinaryOnWindows(binName: String): Path? {
        try {
            val pb = ProcessBuilder("where", binName)
            val process = pb.start()
            if (process.waitFor(1, TimeUnit.SECONDS) && process.exitValue() == 0) {
                val path = String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .lines().firstOrNull()?.trim()
                if (path != null) {
                    return Paths.get(path)
                }
            }
        } catch (e: Exception) {
            LOG.debug("Failed to locate rg using 'where' command", e)
        }

        // Check common installation locations on Windows
        val commonPaths = listOf(
            Paths.get(System.getenv("ProgramFiles"), "ripgrep", binName),
            Paths.get(System.getenv("ProgramFiles(x86)"), "ripgrep", binName),
            Paths.get(System.getenv("USERPROFILE"), ".cargo", "bin", binName)
        )

        for (path in commonPaths) {
            if (path.toFile().exists()) {
                return path
            }
        }

        return findInPath(binName)
    }

    private fun findRipgrepBinaryOnUnix(binName: String): Path? {
        if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")) {
            // Check common macOS installation paths in order of preference
            val macPaths = listOf(
                "/opt/homebrew/bin/rg",        // Apple Silicon Homebrew
                "/usr/local/bin/rg",           // Intel Homebrew / manual install
                "/opt/homebrew/sbin/rg",       // Alternative Homebrew location
                "/usr/local/sbin/rg",          // Alternative manual install location
                System.getProperty("user.home") + "/.cargo/bin/rg"  // Cargo install
            )

            LOG.debug("Checking macOS-specific paths for ripgrep: $macPaths")

            for (pathStr in macPaths) {
                val path = Paths.get(pathStr)
                LOG.debug("Checking path: $pathStr")
                if (path.toFile().exists() && path.toFile().canExecute()) {
                    LOG.debug("Found ripgrep at macOS path: $path")
                    return path
                }
            }

            LOG.debug("Ripgrep not found in any macOS-specific paths")
        }

        // Try using 'which' command to find the binary in PATH
        try {
            val pb = ProcessBuilder("which", binName)
            val process = pb.start()
            if (process.waitFor(1, TimeUnit.SECONDS) && process.exitValue() == 0) {
                val path = String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8).trim()
                if (path.isNotEmpty()) {
                    val rgPath = Paths.get(path)
                    if (rgPath.toFile().exists() && rgPath.toFile().canExecute()) {
                        return rgPath
                    }
                }
            }
        } catch (e: Exception) {
            LOG.debug("Failed to locate rg using 'which' command", e)
        }

        // Fallback to manual PATH search
        return findInPath(binName)
    }

    private fun findInPath(executable: String): Path? {
        val pathEnv = System.getenv("PATH") ?: return null
        val pathSeparator = if (System.getProperty("os.name").lowercase().contains("win")) ";" else ":"

        for (dir in pathEnv.split(pathSeparator)) {
            if (dir.isBlank()) continue
            try {
                val path = Paths.get(dir, executable)
                if (path.toFile().exists() && path.toFile().canExecute()) {
                    LOG.debug("Found ripgrep binary at: $path")
                    return path
                }
            } catch (e: Exception) {
                LOG.debug("Error checking path $dir for $executable", e)
            }
        }

        LOG.debug("Ripgrep binary not found in PATH. PATH=$pathEnv")
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
        basePath: String? = null,
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

    private fun formatResults(searchResults: MutableList<RipgrepSearchResult>, basePath: String): String {
        val output = StringBuilder()
        val grouped: MutableMap<String?, MutableList<RipgrepSearchResult?>?> =
            LinkedHashMap<String?, MutableList<RipgrepSearchResult?>?>()

        var results = searchResults
        output.append("Total results: ").append(results.size)
        if (results.size > 30) {
            results = results.subList(0, 30)
            output.append("Too many results, only show first 30 results\n")
        }

        for (result in results) {
            val relPath = getRelativePath(basePath, result.filePath!!)
            grouped.computeIfAbsent(relPath) { k: String? -> ArrayList<RipgrepSearchResult?>() }!!.add(result)
        }
        output.append("\n```bash\n")
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

        output.append("```\n")
        return output.toString()
    }

    private fun getRelativePath(basePath: String, absolutePath: String): String {
        val base = Paths.get(basePath)
        val target = Paths.get(absolutePath)
        return base.relativize(target).toString().replace('\\', '/')
    }
}
