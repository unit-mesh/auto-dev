package cc.unitmesh.devti.bridge.command

import com.intellij.util.io.awaitExit
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

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

class SccWrapper(
    private val sccPath: String = "scc", private val timeoutSeconds: Long = 60
) {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * 同步执行 scc 命令
     * @param arguments scc 命令行参数
     */
    fun runSccSync(vararg arguments: String): List<SccResult> {
        val command = buildCommand(arguments)
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.reader().use { reader ->
            reader.readText()
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw SccException("scc exited with code $exitCode. Output: $output")
        }

        return parseResult(output)
    }

    /**
     * 异步执行 scc 命令（使用协程）
     * @param arguments scc 命令行参数
     */
    suspend fun runSccAsync(vararg arguments: String): List<SccResult> =
        withContext(Dispatchers.IO) {
            val command = buildCommand(arguments)
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.reader().use { reader ->
                reader.readText()
            }

            val exitCode = withTimeoutOrNull(timeoutSeconds * 1000) {
                process.awaitExit()
            } ?: throw SccException("scc execution timed out after $timeoutSeconds seconds")

            if (exitCode != 0) {
                throw SccException("scc exited with code $exitCode. Output: $output")
            }

            parseResult(output)
        }

    private fun buildCommand(arguments: Array<out String>): List<String> {
        val baseCommand = if (sccPath.contains(" ")) {
            listOf("cmd", "/c", sccPath)
        } else {
            listOf(sccPath)
        }

        return baseCommand + arguments.toList() + "--format" + "json"
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
