package cc.unitmesh.devti.sketch.ui.plan

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class MarkdownPlanParserTest {

    @Test
    fun should_parse_markdown_with_single_section_and_tasks() {
        // Given
        val markdownContent = """
            1. 领域模型重构：
              - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
              - 添加领域行为方法（发布、审核、评论等）
        """.trimIndent()

        // When
        val planItems = MarkdownPlanParser.parse(markdownContent)

        // Then
        assertThat(planItems).hasSize(1)
        assertThat(planItems[0].title).isEqualTo("领域模型重构：")
        assertThat(planItems[0].tasks).containsExactly(
            "将BlogPost实体合并到Blog聚合根，建立完整的领域对象",
            "添加领域行为方法（发布、审核、评论等）"
        )
    }

    @Test
    fun should_parse_markdown_with_multiple_sections_and_tasks() {
        // Given
        val markdownContent = """
            1. 领域模型重构：
              - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
              - 添加领域行为方法（发布、审核、评论等）

            2. 分层结构调整：
              - 清理entity层冗余对象
        """.trimIndent()

        // When
        val planItems = MarkdownPlanParser.parse(markdownContent)

        // Then
        assertThat(planItems).hasSize(2)
        assertThat(planItems[0].title).isEqualTo("领域模型重构：")
        assertThat(planItems[0].tasks).containsExactly(
            "将BlogPost实体合并到Blog聚合根，建立完整的领域对象",
            "添加领域行为方法（发布、审核、评论等）"
        )
        assertThat(planItems[1].title).isEqualTo("分层结构调整：")
        assertThat(planItems[1].tasks).containsExactly("清理entity层冗余对象")
    }

    @Test
    fun should_return_empty_list_when_markdown_content_is_empty() {
        // Given
        val markdownContent = ""

        // When
        val planItems = MarkdownPlanParser.parse(markdownContent)

        // Then
        assertThat(planItems).isEmpty()
    }

    @Test
    fun should_return_empty_list_when_markdown_content_is_invalid() {
        // Given
        val markdownContent = """
            This is not a valid markdown plan
            - Invalid task 1
            - Invalid task 2
        """.trimIndent()

        // When
        val planItems = MarkdownPlanParser.parse(markdownContent)

        // Then
        assertThat(planItems).isEmpty()
    }
}
