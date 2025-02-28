package cc.unitmesh.devti.bridge.command

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.util.Key
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

@Serializable
data class SccResult(
    @SerialName("Name") val name: String,
    @SerialName("Bytes") val bytes: Int,
    @SerialName("CodeBytes") val codeBytes: Int,
    @SerialName("Lines") val lines: Int,
    @SerialName("Code") val code: Int,
    @SerialName("Comment") val comment: Int,
    @SerialName("Blank") val blank: Int,
    @SerialName("Complexity") val complexity: Int,
    @SerialName("Count") val count: Int,
    @SerialName("WeightedComplexity") val weightedComplexity: Int,
    @SerialName("Files") val files: List<FileInfo>,
    @SerialName("LineLength") val lineLength: Int?
)

@Serializable
data class FileInfo(
    @SerialName("Location") val location: String,
    @SerialName("Filename") val filename: String,
    @SerialName("Lines") val lines: Long,
    @SerialName("Code") val code: Long,
    @SerialName("Comment") val comment: Long,
    @SerialName("Blank") val blank: Long,
    @SerialName("Complexity") val complexity: Long
)

class SccWrapper(private val timeoutMs: Long = 30000) {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * 同步执行 scc 命令
     * @param arguments scc 命令行参数
     */
    fun runSccSync(vararg arguments: String): List<SccResult> {
        val command = buildCommand(arguments)
        val handler: OSProcessHandler = ColoredProcessHandler(command)

        handler.startNotify()
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        handler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (outputType == ProcessOutputTypes.STDOUT) {
                    stdout.append(event.text)
                } else if (outputType == ProcessOutputTypes.STDERR) {
                    stderr.append(event.text)
                }
            }
        })

        if (!handler.waitFor(timeoutMs)) {
            throw IOException("SCC command timed out")
        }

        if (handler.exitCode != 0) {
            throw IOException("SCC command failed with exit code ${handler.exitCode}: $stderr")
        }

        return parseResult(stdout.toString())
    }

    private fun buildCommand(arguments: Array<out String>): GeneralCommandLine {
        val sccPath = findSccBinary()
            ?: throw SccException("scc binary not found, please install it first: https://ide.unitmesh.cc/bridge")

        val cmd = GeneralCommandLine(sccPath.toString())

        cmd.addParameters(arguments.toList())
        cmd.addParameters("--format", "json")

        cmd.charset = StandardCharsets.UTF_8
        return cmd
    }

    @Throws(IOException::class)
    fun findSccBinary(): Path? {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        val binName = if (osName.contains("win")) "scc.exe" else "scc"

        // try get from /usr/local/bin/rg if macos
        if (osName.contains("mac")) {
            val path = Paths.get("/usr/local/bin/scc")
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


    private fun parseResult(json: String): List<SccResult> {
        return try {
            jsonParser.decodeFromString<List<SccResult>>(json)
        } catch (e: Exception) {
            throw SccException("Failed to parse scc output", e)
        }
    }

    // 快捷方法示例
    fun analyzeDirectory(directory: String, excludeDirs: List<String> = emptyList()): List<SccResult> {
        val args = mutableListOf<String>().apply {
            add(directory)
            if (excludeDirs.isNotEmpty()) {
                add("--exclude-dir")
                add(excludeDirs.joinToString(","))
            }
        }

        return runSccSync(*args.toTypedArray())
    }
}

class SccException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
