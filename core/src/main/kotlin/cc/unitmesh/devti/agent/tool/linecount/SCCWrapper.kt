package cc.unitmesh.devti.agent.tool.linecount

import com.intellij.execution.configurations.GeneralCommandLine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

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

class SccWrapper(): CmdWrapper<SccResult> {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    override fun buildCommand(arguments: Array<out String>): GeneralCommandLine {
        val sccPath = findBinary("scc")
            ?: throw SccException("scc binary not found, please install it first: https://ide.unitmesh.cc/bridge")

        val cmd = GeneralCommandLine(sccPath.toString())

        cmd.addParameters(arguments.toList())
        cmd.addParameters("--format", "json")

        cmd.charset = StandardCharsets.UTF_8
        return cmd
    }

    override fun parseResult(json: String): List<SccResult> {
        return try {
            jsonParser.decodeFromString<List<SccResult>>(json)
        } catch (e: Exception) {
            throw SccException("Failed to parse scc output", e)
        }
    }
}

class SccException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
