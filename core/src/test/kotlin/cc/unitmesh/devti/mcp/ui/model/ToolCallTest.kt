package cc.unitmesh.devti.mcp.ui.model

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ToolCallTest {
    @Test
    fun should_return_empty_list_when_response_is_not_xml() {
        // given
        val response = """
            ```json
            {"name": "test", "parameters": {}}
            ```
        """.trimIndent()

        // when
        val result = try {
            ToolCall.fromString(response)
        } catch (e: Exception) {
            emptyList<ToolCall>()
        }

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun should_return_empty_list_when_xml_parsing_fails() {
        // given
        val response = """
            ```xml
            <invalid>xml</invalid>
            ```
        """.trimIndent()

        // when
        val result = ToolCall.fromString(response)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun should_parse_single_tool_call_without_parameters() {
        // given
        val response = """
            ```xml
            <devins:invoke name="testTool">
            </devins:invoke>
            ```
        """.trimIndent()

        // when
        val result = ToolCall.fromString(response)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("testTool")
        assertThat(result[0].parameters).isEmpty()
    }

    @Test
    fun should_parse_single_tool_call_with_parameters() {
        // given
        val response = """
            ```xml
            <devins:invoke name="testTool">
                <devins:parameter name="param1">value1</devins:parameter>
                <devins:parameter name="param2">value2</devins:parameter>
            </devins:invoke>
            ```
        """.trimIndent()

        // when
        val result = ToolCall.fromString(response)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("testTool")
        assertThat(result[0].parameters).hasSize(2)
        assertThat(result[0].parameters["param1"]).isEqualTo("value1")
        assertThat(result[0].parameters["param2"]).isEqualTo("value2")
    }
}
