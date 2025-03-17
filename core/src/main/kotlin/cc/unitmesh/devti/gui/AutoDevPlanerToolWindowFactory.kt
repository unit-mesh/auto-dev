package cc.unitmesh.devti.gui

import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.sketch.ui.plan.PlanSketch
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splittable
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicBoolean

class AutoDevPlanerToolWindowFactory : ToolWindowFactory, ToolWindowManagerListener, DumbAware {
    private val orientation = AtomicBoolean(true)

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val panel = AutoDevPlanerTooWindow(project)
        val manager = toolWindow.contentManager
        manager.addContent(manager.factory.createContent(panel, null, false).apply { isCloseable = false })
        project.messageBus.connect(manager).subscribe(ToolWindowManagerListener.TOPIC, this)
    }

    override fun stateChanged(manager: ToolWindowManager) {
        val window = manager.getToolWindow(PlANNER_ID) ?: return
        if (window.isDisposed) return
        val vertical = !window.anchor.isHorizontal
        if (vertical != orientation.getAndSet(vertical)) {
            for (content in window.contentManager.contents) {
                val splittable = content?.component as? Splittable
                splittable?.orientation = vertical
            }
        }
    }

    companion object {
        val PlANNER_ID = "AutoDevPlaner"
    }
}

class AutoDevPlanerTooWindow(val project: Project) : SimpleToolWindowPanel(true, true), Disposable {
    override fun getName(): @NlsActions.ActionText String? = "AutoDev Planer"
    var connection = ApplicationManager.getApplication().messageBus.connect(this)

    val content = """1. 更新实体类 BlogPost
    - [✓] 添加 category 字段
    - [✓] 更新构造函数
    - [✓] 添加 getter 和 setter

2. 更新 DTO 类 CreateBlogRequest
    - [✓] 添加 category 字段

3. 更新 BlogService
    - [✓] 确保 createBlog 方法正确处理 category 字段

4. 更新 BlogRepository
    - [✓] 添加 findByCategory 方法

5. 更新 BlogController
    - [✓] 添加按 category 查询的 API 端点
    - [✓] 更新 Swagger 文档

6. 测试验证
    - [ ] 编写 Postman 测试用例
    - [ ] 创建带 category 参数的集成测试
    - [ ] 验证空分类场景处理
"""
    var planSketch: PlanSketch = PlanSketch(project, content, MarkdownPlanParser.parse(content).toMutableList(), true)

    init {
        val planPanel = panel {
            row {
                cell(planSketch)
                    .fullWidth()
                    .resizableColumn()
            }
        }.apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(8)
            )
            background = JBUI.CurrentTheme.ToolWindow.background()
        }

        add(planPanel, BorderLayout.CENTER)

        connection.subscribe(PlanUpdateListener.TOPIC, object : PlanUpdateListener {
            override fun onPlanUpdate(items: MutableList<AgentTaskEntry>) {
                planSketch.updatePlan(items)
            }
        })
    }

    override fun dispose() {

    }
}