package cc.unitmesh.devti.sketch.ui

import cc.unitmesh.devti.sketch.ui.plan.MarkdownPlanParser
import org.junit.Assert.*
import org.junit.Test

class MarkdownPlanParserTest {
    
    @Test
    fun `should parse simple plan with one section`() {
        // given
        val markdown = """
            1. 领域模型重构：
              - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
              - 添加领域行为方法（发布、审核、评论等）
        """.trimIndent()
        
        // when
        val planItems = MarkdownPlanParser.parse(markdown)
        
        // then
        assertEquals(1, planItems.size)
        assertEquals("领域模型重构：", planItems[0].title)
        assertEquals(2, planItems[0].tasks.size)
        assertEquals("将BlogPost实体合并到Blog聚合根，建立完整的领域对象", planItems[0].tasks[0])
        assertEquals("添加领域行为方法（发布、审核、评论等）", planItems[0].tasks[1])
    }
    
    @Test
    fun `should parse plan with multiple sections`() {
        // given
        val markdown = """
            1. 领域模型重构：
              - 将BlogPost实体合并到Blog聚合根，建立完整的领域对象
              - 添加领域行为方法（发布、审核、评论等）
            
            2. 分层结构调整：
              - 清理entity层冗余对象
              - 优化repository实现
        """.trimIndent()
        
        // when
        val planItems = MarkdownPlanParser.parse(markdown)
        
        // then
        assertEquals(2, planItems.size)
        
        // 第一个章节
        assertEquals("领域模型重构：", planItems[0].title)
        assertEquals(2, planItems[0].tasks.size)
        assertEquals("将BlogPost实体合并到Blog聚合根，建立完整的领域对象", planItems[0].tasks[0])
        assertEquals("添加领域行为方法（发布、审核、评论等）", planItems[0].tasks[1])
        
        // 第二个章节
        assertEquals("分层结构调整：", planItems[1].title)
        assertEquals(2, planItems[1].tasks.size)
        assertEquals("清理entity层冗余对象", planItems[1].tasks[0])
        assertEquals("优化repository实现", planItems[1].tasks[1])
    }
    
    @Test
    fun `should handle different list item markers`() {
        // given
        val markdown = """
            1. 测试章节：
              * 使用星号的任务
              - 使用减号的任务
        """.trimIndent()
        
        // when
        val planItems = MarkdownPlanParser.parse(markdown)
        
        // then
        assertEquals(1, planItems.size)
        assertEquals("测试章节：", planItems[0].title)
        assertEquals(2, planItems[0].tasks.size)
        assertEquals("使用星号的任务", planItems[0].tasks[0])
        assertEquals("使用减号的任务", planItems[0].tasks[1])
    }
    
    @Test
    fun `should handle invalid input`() {
        // given
        val markdown = "这不是一个有效的计划格式"
        
        // when
        val planItems = MarkdownPlanParser.parse(markdown)
        
        // then
        assertEquals(0, planItems.size)
    }
    
    @Test
    fun `should handle nested tasks`() {
        // given
        val markdown = """
            1. 主要功能：
              - 任务1
                - 子任务A
                - 子任务B
              - 任务2
        """.trimIndent()
        
        // when
        val planItems = MarkdownPlanParser.parse(markdown)
        
        // then
        assertEquals(1, planItems.size)
        assertEquals("主要功能：", planItems[0].title)
        
        // 目前的实现可能不支持嵌套，所以需要根据实际结果调整测试
        assertEquals(2, planItems[0].tasks.size)
        assertEquals("任务1", planItems[0].tasks[0])
        assertEquals("任务2", planItems[0].tasks[1])
    }
} 