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

    @Test
    fun should_return_current_list_when_markdown_content_is_correnct() {
        // Given
        val markdownContent = """
            1. 分析现有代码结构 ✓
               - 确认Blog相关实体、控制器、服务层结构 ✓
            2. 确定功能实现路径
               - 数据库层：BlogRepository添加根据作者删除方法 ✓
               - 服务层：BlogService添加删除逻辑 ✓
               - 控制层：BlogController添加新端点 ✓
            3. 实现数据库操作
               - 在BlogRepository添加deleteByAuthor方法 ✓
            4. 完善服务层逻辑
               - 处理删除结果和异常情况 ✓
            5. 添加API端点
               - 创建DELETE方法接收author参数 ✓
            6. 验证数据库表结构
               - 确保blog_post表存在author字段 ✓
            7. 测试功能
               - 使用curl或Postman验证接口 ✓    
        """.trimIndent()

        // When
        val planItems = MarkdownPlanParser.parse(markdownContent)

        // Then
        assertThat(planItems).hasSize(7)
        assertThat(planItems[0].title).isEqualTo("分析现有代码结构")
        assertThat(planItems[0].tasks).containsExactly(
            "确认Blog相关实体、控制器、服务层结构 ✓"
        )
        assertThat(planItems[1].title).isEqualTo("确定功能实现路径")
        assertThat(planItems[1].tasks).containsExactly(
            "数据库层：BlogRepository添加根据作者删除方法 ✓",
            "服务层：BlogService添加删除逻辑 ✓",
            "控制层：BlogController添加新端点 ✓"
        )
    }
}
