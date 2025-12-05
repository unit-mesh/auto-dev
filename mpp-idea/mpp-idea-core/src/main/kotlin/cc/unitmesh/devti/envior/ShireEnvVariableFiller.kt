package cc.unitmesh.devti.envior

import com.intellij.json.JsonUtil
import com.intellij.json.psi.*
import com.intellij.openapi.util.TextRange

object ShireEnvVariableFiller {
    private fun getVariableValue(jsonObject: JsonObject, name: String, processVars: Map<String, String>): String? {
        val value = JsonUtil.getPropertyValueOfType(jsonObject, name, JsonLiteral::class.java)
        val jsonResult = getValueAsString(value)
        if (jsonResult != null) {
            return jsonResult
        }

        return processVars[name]
    }

    private fun getValueAsString(value: JsonLiteral?): String? {
        return when (value) {
            is JsonStringLiteral -> value.value
            is JsonBooleanLiteral -> value.value.toString()
            else -> value?.text
        }
    }

    fun fillVariables(
        messageBody: String,
        variables: List<Set<String>>,
        obj: JsonObject?,
        processVars: Map<String, String>
    ): String {
        if (obj == null) return messageBody
        if (variables.isEmpty()) return messageBody

        val envRanges = collectVariablesRangesInMessageBody(messageBody)

        val result = StringBuilder(messageBody.length)
        var lastVariableRangeEndOffset = 0

        for (variableRange in envRanges) {
            result.append(messageBody as CharSequence, lastVariableRangeEndOffset, variableRange.startOffset)
            val variableValue = getVariableValue(obj, getVariableKey(variableRange, messageBody), processVars)

            result.append(variableValue)
            lastVariableRangeEndOffset = variableRange.endOffset
        }

        result.append(messageBody as CharSequence, lastVariableRangeEndOffset, messageBody.length)
        val sb = result.toString()
        return sb
    }

    private fun getVariableKey(variableRange: TextRange, messageBody: String) =
        variableRange.substring(messageBody).removePrefix("\${").removeSuffix("}")

    private fun collectVariablesRangesInMessageBody(body: String): List<TextRange> {
        val ranges = mutableListOf<TextRange>()
        var startIndex = 0

        while (startIndex < body.length) {
            val openBraceIndex = body.indexOf("\${", startIndex)
            val closeBraceIndex = body.indexOf("}", openBraceIndex)

            if (openBraceIndex == -1 || closeBraceIndex == -1) {
                break
            }

            val range = TextRange(openBraceIndex, closeBraceIndex + 1)
            val contentInsideBraces = body.substring(openBraceIndex + 2, closeBraceIndex)

            if (contentInsideBraces.isNotBlank()) {
                ranges.add(range)
            }

            startIndex = closeBraceIndex + 1
        }

        return ranges
    }
}