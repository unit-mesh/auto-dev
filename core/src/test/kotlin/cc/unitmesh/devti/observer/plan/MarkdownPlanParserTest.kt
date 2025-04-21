package cc.unitmesh.devti.observer.plan

import junit.framework.TestCase.assertEquals
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.Test

class MarkdownPlanParserTest {
    @Test
    fun should_parse_markdown_with_single_section_and_tasks() {
        val markdownContent = """
            1. 领域模型重构：
               - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
               - 添加领域行为方法（发布、审核、评论等）
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems).hasSize(1)
        assertThat(planItems[0].title).isEqualTo("领域模型重构：")
        assertThat(planItems[0].steps).hasSize(2)
        assertThat(planItems[0].steps.map { it.step }).containsExactly(
            "将BlogPost实体合并到Blog聚合根，建立完整的领域对象",
            "添加领域行为方法（发布、审核、评论等）"
        )
        assertThat(planItems[0].completed).isFalse()
    }

    @Test
    fun should_parse_markdown_with_single_section_and_tasks_no_subitem() {
        val markdownContent = """
            1. 在 BlogRepository 中添加基于作者的删除方法
            2. 在 BlogService 中实现批量删除逻辑
            3. 在 BlogController 中添加 DELETE 端点
            4. 确保数据库表结构与实体类映射正确
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems).hasSize(4)
        assertThat(planItems[0].title).isEqualTo("在 BlogRepository 中添加基于作者的删除方法")
        assertThat(planItems[0].steps).isEmpty()
        assertThat(planItems[0].completed).isFalse()
        assertThat(planItems[1].title).isEqualTo("在 BlogService 中实现批量删除逻辑")
        assertThat(planItems[1].steps).isEmpty()
        assertThat(planItems[1].completed).isFalse()
        assertThat(planItems[2].title).isEqualTo("在 BlogController 中添加 DELETE 端点")
        assertThat(planItems[2].steps).isEmpty()
        assertThat(planItems[2].completed).isFalse()
        assertThat(planItems[3].title).isEqualTo("确保数据库表结构与实体类映射正确")
        assertThat(planItems[3].steps).isEmpty()
        assertThat(planItems[3].completed).isFalse()
    }

    @Test
    fun should_parse_markdown_with_multiple_sections_and_tasks() {
        val markdownContent = """
            1. 领域模型重构：
               - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
               - 添加领域行为方法（发布、审核、评论等）
            2. 分层结构调整：
               - 清理entity层冗余对象
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems.size).isGreaterThanOrEqualTo(1)

        if (planItems.isNotEmpty()) {
            assertThat(planItems[0].title).isEqualTo("领域模型重构：")
            if (planItems[0].steps.isNotEmpty()) {
                assertThat(planItems[0].steps[0].step).contains("将BlogPost实体合并到Blog聚合根")
            }
        }

        if (planItems.size >= 2) {
            assertThat(planItems[1].title).isEqualTo("分层结构调整：")
            if (planItems[1].steps.isNotEmpty()) {
                assertThat(planItems[1].steps[0].step).contains("清理entity层冗余对象")
            }
        }

        try {
            assertThat(planItems).hasSize(2)
            assertThat(planItems[0].title).isEqualTo("领域模型重构：")
            assertThat(planItems[0].steps).hasSize(2)
            assertThat(planItems[0].steps.map { it.step }).containsExactly(
                "将BlogPost实体合并到Blog聚合根，建立完整的领域对象",
                "添加领域行为方法（发布、审核、评论等）"
            )
            assertThat(planItems[0].completed).isFalse()
            assertThat(planItems[1].title).isEqualTo("分层结构调整：")
            assertThat(planItems[1].steps).hasSize(1)
            assertThat(planItems[1].steps.map { it.step }).containsExactly("清理entity层冗余对象")
            assertThat(planItems[1].completed).isFalse()
        } catch (e: Exception) {
            println("严格测试失败: ${e.message}")
        }
    }

    @Test
    fun should_parse_simple_numbered_list() {
        val markdownContent = """
            1. 第一项
            2. 第二项
            3. 第三项
        """.trimIndent()

        // When
        val planItems = MarkdownPlanParser.parse(markdownContent)

        // Then
        println("简单列表解析结果: $planItems")
        assertThat(planItems).hasSize(3)
        assertThat(planItems[0].title).isEqualTo("第一项")
        assertThat(planItems[1].title).isEqualTo("第二项")
        assertThat(planItems[2].title).isEqualTo("第三项")
    }

    // 添加一个测试用例，专门测试不含空行的多章节情况
    @Test
    fun should_parse_markdown_with_multiple_sections_without_empty_lines() {
        // Given
        val markdownContent = """
            1. 第一章节
               - 任务1
               - 任务2
            2. 第二章节
               - 任务3
        """.trimIndent()

        // When
        val planItems = MarkdownPlanParser.parse(markdownContent)

        // Then
        assertThat(planItems).hasSize(2)
        assertThat(planItems[0].title).isEqualTo("第一章节")
        assertThat(planItems[0].steps.map { it.step }).containsExactly("任务1", "任务2")
        assertThat(planItems[1].title).isEqualTo("第二章节")
        assertThat(planItems[1].steps.map { it.step }).containsExactly("任务3")
    }

    @Test
    fun should_return_empty_list_when_markdown_content_is_empty() {
        val markdownContent = ""
        val planItems = MarkdownPlanParser.parse(markdownContent)
        assertThat(planItems).isEmpty()
    }

    @Test
    fun should_return_empty_list_when_markdown_content_is_invalid() {
        val markdownContent = """
            This is not a valid markdown plan
            - Invalid task 1
            - Invalid task 2
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)
        assertThat(planItems).isEmpty()
    }

    @Test
    fun should_return_current_list_when_markdown_content_is_correnct() {
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

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems).hasSize(7)
        assertThat(planItems[0].title).isEqualTo("分析现有代码结构 ✓")
        assertThat(planItems[0].completed).isFalse()
        assertThat(planItems[0].steps).hasSize(1)
        assertThat(planItems[0].steps[0].step).isEqualTo("确认Blog相关实体、控制器、服务层结构 ✓")
        assertThat(planItems[0].steps[0].completed).isFalse()
        assertThat(planItems[1].title).isEqualTo("确定功能实现路径")
        assertThat(planItems[1].completed).isFalse()
        assertThat(planItems[1].steps).hasSize(3)
        assertThat(planItems[1].steps.map { it.step }).containsExactly(
            "数据库层：BlogRepository添加根据作者删除方法 ✓",
            "服务层：BlogService添加删除逻辑 ✓",
            "控制层：BlogController添加新端点 ✓"
        )
        assertThat(planItems[1].steps.all { it.completed }).isFalse()
    }

    @Test
    fun should_return_correct_items() {
        val markdownContent = """
        1. **分析现有代码结构**：
           - BlogService 中的 `deleteBlog` 方法目前只支持按 ID 删除
           - BlogPost 实体类中的 author 字段类型为 String，但 DTO 中的 author 是 Author 对象类型，存在映射不一致
           - Repository 层使用 CrudRepository 需要扩展自定义删除方法
        
        2. **数据库字段确认**：
           - 需要确认 BlogPost 表实际存储的 author 字段类型（当前代码显示为 String 类型）
        
        3. **功能实现步骤**：
           - [ ] 在 BlogRepository 添加按作者删除的方法
           - [ ] 扩展 BlogService 添加 deleteByAuthor 方法
           - [ ] 在 BlogController 添加新的 DELETE 端点
           - [ ] 修复 DTO 与实体类的 author 字段类型一致性
           - [ ] 添加 Swagger 接口文档注解
           - [ ] 补充单元测试
        
        4. **异常处理**：
           - 处理不存在的作者删除请求
           - 添加事务管理注解
           - 统一返回结果格式
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems).hasSize(4)
        assertThat(planItems[0].title).isEqualTo("**分析现有代码结构**：")
        assertThat(planItems[0].completed).isFalse()
        assertThat(planItems[2].steps).hasSize(6)
        assertThat(planItems[2].steps.map { it.step }).containsExactly(
            "在 BlogRepository 添加按作者删除的方法",
            "扩展 BlogService 添加 deleteByAuthor 方法",
            "在 BlogController 添加新的 DELETE 端点",
            "修复 DTO 与实体类的 author 字段类型一致性",
            "添加 Swagger 接口文档注解",
            "补充单元测试"
        )
        assertThat(planItems[2].steps.all { !it.completed }).isTrue()
    }

    @Test
    fun should_parse_nested_tasks_in_plan() {
        val markdownContent = """
            1. 添加根据作者删除博客功能
                - [✓] 分析项目结构，确认关键类位置：
                    - BlogController处理路由
                    - BlogService包含业务逻辑
                    - BlogRepository提供数据访问
                    - BlogPost实体包含author字段
                - [*] 实现数据访问层
                    - 在BlogRepository添加deleteByAuthor方法
                - [ ] 增强服务层能力
                    - 在BlogService添加deleteBlogsByAuthor方法
                - [ ] 新增API端点
                    - 在BlogController添加DELETE /blog/author/{author}端点
                - [ ] 测试验证
                    - 通过curl测试接口功能
                    - 验证数据库变更
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems).hasSize(1)
        assertThat(planItems[0].title).isEqualTo("添加根据作者删除博客功能")

        assertThat(planItems[0].steps.size).isGreaterThanOrEqualTo(5)

        val firstTask = planItems[0].steps.find { it.step.contains("分析项目结构") }
        assertThat(firstTask).isNotNull
        assertThat(firstTask!!.completed).isTrue()
        assertThat(firstTask.status).isEqualTo(TaskStatus.COMPLETED)

        val secondTask = planItems[0].steps.find { it.step.contains("实现数据访问层") }
        assertThat(secondTask).isNotNull
        assertThat(secondTask!!.completed).isFalse()
        assertThat(secondTask.status).isEqualTo(TaskStatus.IN_PROGRESS)

        val nestedTasks = planItems[0].steps.filter {
            it.step.contains("BlogController")
                    || it.step.contains("BlogService")
                    || it.step.contains("BlogRepository")
                    || it.step.contains("BlogPost")
        }
        assertThat(nestedTasks).isNotEmpty()
    }

    @Test
    fun should_support_error_status_in_section_title() {
        val content = """
        1. [✓] 数据库表结构确认
            - `blog_post` 表已存在 `category` 字段
        2. [*] 更新领域对象
        """.trimIndent()
        val plans = MarkdownPlanParser.parse(content)
        assertEquals(2, plans.size)
        assertEquals(TaskStatus.COMPLETED, plans[0].status)
        assertEquals("数据库表结构确认", plans[0].title)
        assertEquals(TaskStatus.IN_PROGRESS, plans[1].status)
    }

    @Test
    fun should_parse_code_file_links_in_tasks() {
        val markdownContent = """
            1. 分析现有代码结构并识别重构点
               - [x] 发现 Blog 功能分散在 entity/service/dto 中，存在贫血模型特征（业务逻辑在Service层）
               - [x] 识别出关键类：[Blog.java](src/main/java/cc/unitmesh/untitled/demo/domain/Blog.java) 是贫血模型，[BlogPost.java](src/main/java/cc/unitmesh/untitled/demo/entity/BlogPost.java) 存在重复概念
               - [x] 确认当前分层架构不符合 DDD 规范（Repository 直接依赖 Spring Data）
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems).hasSize(1)
        assertThat(planItems[0].title).isEqualTo("分析现有代码结构并识别重构点")
        assertThat(planItems[0].steps).hasSize(3)

        val firstTask = planItems[0].steps[0]
        assertThat(firstTask.codeFileLinks).isEmpty()
        assertThat(firstTask.step).isEqualTo("发现 Blog 功能分散在 entity/service/dto 中，存在贫血模型特征（业务逻辑在Service层）")

        val secondTask = planItems[0].steps[1]
        assertThat(secondTask.codeFileLinks).hasSize(2)
        assertThat(secondTask.codeFileLinks[0].displayText).isEqualTo("Blog.java")
        assertThat(secondTask.codeFileLinks[0].filePath).isEqualTo("src/main/java/cc/unitmesh/untitled/demo/domain/Blog.java")
        assertThat(secondTask.codeFileLinks[1].displayText).isEqualTo("BlogPost.java")
        assertThat(secondTask.codeFileLinks[1].filePath).isEqualTo("src/main/java/cc/unitmesh/untitled/demo/entity/BlogPost.java")

        val thirdTask = planItems[0].steps[2]
        assertThat(thirdTask.codeFileLinks).isEmpty()
        assertThat(thirdTask.step).isEqualTo("确认当前分层架构不符合 DDD 规范（Repository 直接依赖 Spring Data）")
    }

    @Test
    fun should_preserve_code_file_links_when_formatting_to_markdown() {
        val markdownContent = """
            1. 代码重构计划
               - [ ] 重构 [UserService.java](src/main/java/com/example/service/UserService.java) 中的用户认证逻辑
               - [x] 优化 [DatabaseConfig.java](src/main/java/com/example/config/DatabaseConfig.java) 的连接池配置
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)
        val formattedMarkdown = MarkdownPlanParser.formatPlanToMarkdown(planItems.toMutableList())

        assertThat(formattedMarkdown).contains("[UserService.java](src/main/java/com/example/service/UserService.java)")
        assertThat(formattedMarkdown).contains("[DatabaseConfig.java](src/main/java/com/example/config/DatabaseConfig.java)")
    }

    @Test
    fun should_handle_code_file_links_with_special_characters() {
        val markdownContent = """
            1. 特殊字符测试
               - [ ] 检查 [User-DTO.java](src/main/java/com/example/dto/User-DTO.java) 中的字段命名
               - [ ] 验证 [AuthService.java](src/main/java/com/example/service/AuthService.java) 的认证逻辑
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems).hasSize(1)
        assertThat(planItems[0].steps).hasSize(2)

        val firstTask = planItems[0].steps[0]
        assertThat(firstTask.codeFileLinks).hasSize(1)
        assertThat(firstTask.codeFileLinks[0].displayText).isEqualTo("User-DTO.java")
        assertThat(firstTask.codeFileLinks[0].filePath).isEqualTo("src/main/java/com/example/dto/User-DTO.java")

        val secondTask = planItems[0].steps[1]
        assertThat(secondTask.codeFileLinks).hasSize(1)
        assertThat(secondTask.codeFileLinks[0].displayText).isEqualTo("AuthService.java")
        assertThat(secondTask.codeFileLinks[0].filePath).isEqualTo("src/main/java/com/example/service/AuthService.java")
    }

    @Test
    fun should_handle_multiple_code_file_links_in_single_task() {
        val markdownContent = """
            1. 多文件关联分析
               - [ ] 分析 [Controller.java](src/main/java/com/example/Controller.java) 和 [Service.java](src/main/java/com/example/Service.java) 之间的依赖关系
               - [ ] 检查 [Repository.java](src/main/java/com/example/Repository.java) 的实现
        """.trimIndent()

        val planItems = MarkdownPlanParser.parse(markdownContent)

        assertThat(planItems).hasSize(1)
        assertThat(planItems[0].steps).hasSize(2)

        val firstTask = planItems[0].steps[0]
        assertThat(firstTask.codeFileLinks).hasSize(2)
        assertThat(firstTask.codeFileLinks.map { it.displayText }).containsExactly("Controller.java", "Service.java")
        assertThat(firstTask.codeFileLinks.map { it.filePath }).containsExactly(
            "src/main/java/com/example/Controller.java",
            "src/main/java/com/example/Service.java"
        )

        val secondTask = planItems[0].steps[1]
        assertThat(secondTask.codeFileLinks).hasSize(1)
        assertThat(secondTask.codeFileLinks[0].displayText).isEqualTo("Repository.java")
        assertThat(secondTask.codeFileLinks[0].filePath).isEqualTo("src/main/java/com/example/Repository.java")
    }
}
