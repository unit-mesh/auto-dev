package cc.unitmesh.devti.language.compiler.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LineInfoTest {

    @Test
    fun should_create_line_info_with_default_values() {
        // given
        val startLine = 1
        val endLine = 10

        // when
        val lineInfo = LineInfo(startLine, endLine)

        // then
        assertThat(lineInfo.startLine).isEqualTo(1)
        assertThat(lineInfo.endLine).isEqualTo(10)
        assertThat(lineInfo.startColumn).isEqualTo(0)
        assertThat(lineInfo.endColumn).isEqualTo(0)
    }

    @Test
    fun should_create_line_info_with_custom_values() {
        // given
        val startLine = 5
        val endLine = 15
        val startColumn = 10
        val endColumn = 20

        // when
        val lineInfo = LineInfo(startLine, endLine, startColumn, endColumn)

        // then
        assertThat(lineInfo.startLine).isEqualTo(5)
        assertThat(lineInfo.endLine).isEqualTo(15)
        assertThat(lineInfo.startColumn).isEqualTo(10)
        assertThat(lineInfo.endColumn).isEqualTo(20)
    }

    @Test
    fun should_parse_simple_line_format() {
        // given
        val input = "L5"

        // when
        val lineInfo = LineInfo.fromString(input)

        // then
        assertThat(lineInfo).isNotNull
        assertThat(lineInfo!!.startLine).isEqualTo(5)
        assertThat(lineInfo.endLine).isEqualTo(5)
        assertThat(lineInfo.startColumn).isEqualTo(0)
        assertThat(lineInfo.endColumn).isEqualTo(0)
    }

    @Test
    fun should_parse_start_line_with_column_format() {
        // given
        val input = "L5C10"

        // when
        val lineInfo = LineInfo.fromString(input)

        // then
        assertThat(lineInfo).isNotNull
        assertThat(lineInfo!!.startLine).isEqualTo(5)
        assertThat(lineInfo.endLine).isEqualTo(5)
        assertThat(lineInfo.startColumn).isEqualTo(10)
        assertThat(lineInfo.endColumn).isEqualTo(0)
    }

    @Test
    fun should_parse_line_range_format() {
        // given
        val input = "L5-L10"

        // when
        val lineInfo = LineInfo.fromString(input)

        // then
        assertThat(lineInfo).isNotNull
        assertThat(lineInfo!!.startLine).isEqualTo(5)
        assertThat(lineInfo.endLine).isEqualTo(10)
        assertThat(lineInfo.startColumn).isEqualTo(0)
        assertThat(lineInfo.endColumn).isEqualTo(0)
    }

    @Test
    fun should_parse_full_range_format_with_columns() {
        // given
        val input = "L5C10-L10C20"

        // when
        val lineInfo = LineInfo.fromString(input)

        // then
        assertThat(lineInfo).isNotNull
        assertThat(lineInfo!!.startLine).isEqualTo(5)
        assertThat(lineInfo.endLine).isEqualTo(10)
        assertThat(lineInfo.startColumn).isEqualTo(10)
        assertThat(lineInfo.endColumn).isEqualTo(20)
    }

    @Test
    fun should_parse_mixed_format_with_start_column_only() {
        // given
        val input = "L5C10-L10"

        // when
        val lineInfo = LineInfo.fromString(input)

        // then
        assertThat(lineInfo).isNotNull
        assertThat(lineInfo!!.startLine).isEqualTo(5)
        assertThat(lineInfo.endLine).isEqualTo(10)
        assertThat(lineInfo.startColumn).isEqualTo(10)
        assertThat(lineInfo.endColumn).isEqualTo(0)
    }

    @Test
    fun should_return_null_for_invalid_format() {
        // given
        val input = "invalid format"

        // when
        val lineInfo = LineInfo.fromString(input)

        // then
        assertThat(lineInfo).isNull()
    }

    @Test
    fun should_return_null_for_invalid_line_number() {
        // given
        val input = "Labc"

        // when
        val lineInfo = LineInfo.fromString(input)

        // then
        assertThat(lineInfo).isNull()
    }

    @Test
    fun should_handle_input_with_prefix() {
        // given
        val input = "file.txt#L5-L10"

        // when
        val lineInfo = LineInfo.fromString(input)

        // then
        assertThat(lineInfo).isNotNull
        assertThat(lineInfo!!.startLine).isEqualTo(5)
        assertThat(lineInfo.endLine).isEqualTo(10)
        assertThat(lineInfo.startColumn).isEqualTo(0)
        assertThat(lineInfo.endColumn).isEqualTo(0)
    }
}
