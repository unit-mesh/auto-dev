package cc.unitmesh.devti.mcp.ui.model

import cc.unitmesh.devti.util.parser.CodeFence
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class ToolCall(
    val name: String,
    val parameters: Map<String, String>
) {
    companion object {
        fun fromString(response: String): List<ToolCall> {
            val toolCalls = mutableListOf<ToolCall>()

            val codeblock = CodeFence.Companion.parse(response)
            if (codeblock.originLanguage != "xml") {
                return emptyList()
            }

            try {
                val xmlFactory = DocumentBuilderFactory.newInstance()
                val builder = xmlFactory.newDocumentBuilder()
                val inputSource = InputSource(StringReader(codeblock.text))
                val document = builder.parse(inputSource)

                val invokeNodes = document.getElementsByTagName("devins:invoke")
                for (i in 0 until invokeNodes.length) {
                    val invokeNode = invokeNodes.item(i) as Element
                    val toolName = invokeNode.getAttribute("name")

                    val parameters = mutableMapOf<String, String>()
                    val paramNodes = invokeNode.getElementsByTagName("devins:parameter")
                    for (j in 0 until paramNodes.length) {
                        val paramNode = paramNodes.item(j) as Element
                        val paramName = paramNode.getAttribute("name")
                        val paramValue = paramNode.textContent
                        parameters[paramName] = paramValue
                    }

                    toolCalls.add(ToolCall(toolName, parameters))
                }
            } catch (e: Exception) {
                return emptyList()
            }

            return toolCalls
        }
    }
}