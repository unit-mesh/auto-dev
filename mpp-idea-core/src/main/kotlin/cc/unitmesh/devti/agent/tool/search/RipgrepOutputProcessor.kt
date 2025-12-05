package cc.unitmesh.devti.agent.tool.search

import com.google.gson.JsonParser
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key
import kotlinx.serialization.Serializable

@Serializable
public data class RipgrepSearchResult(
    var filePath: String? = null,
    var line: Int = 0,
    var column: Int = 0,
    var match: String? = null,
    var beforeContext: MutableList<String?> = ArrayList<String?>(),
    var afterContext: MutableList<String?> = ArrayList<String?>()
)

class RipgrepOutputProcessor : ProcessAdapter() {
    private val results: MutableList<RipgrepSearchResult> = ArrayList<RipgrepSearchResult>()
    private var currentResult: RipgrepSearchResult? = null

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

                    currentResult = RipgrepSearchResult(
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

    fun getResults(): MutableList<RipgrepSearchResult> {
        if (currentResult != null) {
            results.add(currentResult!!)
        }

        return results
    }
}