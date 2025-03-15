package cc.unitmesh.devti.util.parser

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Ignore
import kotlin.test.Test

class MarkdownConverterTest {

    @Test
    @Ignore
    fun should_convert_simple_markdown_to_html() {
        // Given
        val markdownText = """
            ```
            1. 领域模型重构：
            - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
            - 添加领域行为方法（发布、审核、评论等）
            - 引入值对象（BlogId、Content等）
            
            2. 分层结构调整：
            - 清理entity层冗余对象，建立清晰的domain层
            - 实现领域服务与基础设施层分离
            - 重构数据持久化接口
            
            3. 战术模式实现：
            - 使用工厂模式处理复杂对象创建
            - 实现仓储接口与领域层的依赖倒置
            - 添加领域事件机制
            
            4. 测试保障：
            - 重构单元测试，验证领域模型行为
            - 添加聚合根不变性约束测试
            ```
            """.trimIndent()
        val expectedHtml = "<pre><code class=\"language-plan\">\n" +
                "1. 领域模型重构：\n" +
                "- 将BlogPost实体合并到Blog聚合根，建立完整的领域对象\n" +
                "- 添加领域行为方法（发布、审核、评论等）\n" +
                "- 引入值对象（BlogId、Content等）\n" +
                "\n" +
                "2. 分层结构调整：\n" +
                "- 清理entity层冗余对象，建立清晰的domain层\n" +
                "- 实现领域服务与基础设施层分离\n" +
                "- 重构数据持久化接口\n" +
                "\n" +
                "3. 战术模式实现：\n" +
                "- 使用工厂模式处理复杂对象创建\n" +
                "- 实现仓储接口与领域层的依赖倒置\n" +
                "- 添加领域事件机制\n" +
                "\n" +
                "4. 测试保障：\n" +
                "- 重构单元测试，验证领域模型行为\n" +
                "- 添加聚合根不变性约束测试\n" +
                "</code></pre>"
        // When
        val resultHtml = convertMarkdownToHtml(markdownText)

        // Then
        assertThat(resultHtml).isEqualTo(expectedHtml)
    }
}
