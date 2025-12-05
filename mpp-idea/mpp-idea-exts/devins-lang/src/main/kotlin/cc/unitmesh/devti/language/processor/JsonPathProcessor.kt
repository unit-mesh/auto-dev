package cc.unitmesh.devti.language.processor

import cc.unitmesh.devti.language.ast.action.PatternActionFunc
import cc.unitmesh.devti.language.ast.action.PatternActionFuncDef
import cc.unitmesh.devti.language.ast.action.PatternProcessor
import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jayway.jsonpath.JsonPath

object JsonPathProcessor : PatternProcessor {
    override val type: PatternActionFuncDef = PatternActionFuncDef.JSONPATH

    fun execute(
        project: Project,
        jsonStr: String,
        action: PatternActionFunc.JsonPath,
    ): String? {
        if (action.sseMode || jsonStr.startsWith("data: ")) {
            return parseSSEResult(jsonStr, action.path.trim())
        }

        val result: String = try {
            JsonPath.parse(jsonStr)?.read<Any>(action.path.trim()).toString()
        } catch (e: Exception) {
            console(project,"jsonpath error: $e")
            return null
        }

        if (result == "null") {
            console(project,"jsonpath error: $result for $jsonStr")
            return null
        }

        return result
    }

    fun console(project: Project, str: String) {
        val contentManager = RunContentManager.getInstance(project)
        val console = contentManager.selectedContent?.executionConsole as? ConsoleViewWrapperBase ?: return

        console.print(str, ConsoleViewContentType.ERROR_OUTPUT)
    }


    /**
     * Parses the server-sent events (SSE) result from a given input string using a specified JSON path expression.
     * This method is useful for extracting relevant information from a stream of SSE data.
     *
     * @param input The raw input string containing one or more SSE data lines.
     * @param jsonPath The JSON path expression used to query the JSON data within each data line.
     * @return A string containing the results of applying the JSON path to each data line, separated by newline characters.
     *
     * The method processes the input string as follows:
     * input example
     * ```bash
     * data: {"event": "agent_thought", "conversation_id": "48929266-a58f-46cc-a5eb-33145e6a96ef", "message_id": "91ad550b-1109-4062-88f8-07be18238e0e", "created_at": 1725437154, "task_id": "4f846104-8571-42f1-b04c-f6f034b2fe9e", "id": "cf621bc0-3daa-45ae-9346-9a386f9c73b0", "position": 1, "thought": "", "observation": "", "tool": "", "tool_labels": {}, "tool_input": "", "message_files": []}
     * data: {"event": "agent_message", "conversation_id": "48929266-a58f-46cc-a5eb-33145e6a96ef", "message_id": "91ad550b-1109-4062-88f8-07be18238e0e", "created_at": 1725437154, "task_id": "4f846104-8571-42f1-b04c-f6f034b2fe9e", "id": "91ad550b-1109-4062-88f8-07be18238e0e", "answer": "The"}
     * data: {"event": "message_end", "conversation_id": "48929266-a58f-46cc-a5eb-33145e6a96ef", "message_id": "91ad550b-1109-4062-88f8-07be18238e0e", "created_at": 1725437154, "task_id": "4f846104-8571-42f1-b04c-f6f034b2fe9e", "id": "91ad550b-1109-4062-88f8-07be18238e0e", "metadata": {"usage": {"prompt_tokens": 20, "prompt_unit_price": "0.15", "prompt_price_unit": "0.000001", "prompt_price": "0.0000030", "completion_tokens": 481, "completion_unit_price": "0.60", "completion_price_unit": "0.000001", "completion_price": "0.0002886", "total_tokens": 501, "total_price": "0.0002916", "currency": "USD", "latency": 0.46675555396359414}}}
     * data: {"event": "tts_message_end", "conversation_id": "48929266-a58f-46cc-a5eb-33145e6a96ef", "message_id": "91ad550b-1109-4062-88f8-07be18238e0e", "created_at": 1725437154, "task_id": "4f846104-8571-42f1-b04c-f6f034b2fe9e", "audio": ""}
     */
    fun parseSSEResult(input: String, jsonPath: String): String {
        val lines = input.split("\n")
        val dataLines = lines.filter {
            it.startsWith("data: ")
        }.map {
            it.substring(6)
        }.mapNotNull {
            try {
                JsonPath.parse(it)?.read<Any>(jsonPath)
            } catch (e: Exception) {
                logger<JsonPathProcessor>().warn("jsonpath error: $e")
                null
            }
        }

        return dataLines.joinToString(separator = "")
    }
}