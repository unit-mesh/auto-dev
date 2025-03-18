// filepath: /Volumes/source/ai/autocrud/core/src/main/kotlin/cc/unitmesh/devti/gui/AutoDevPlannerToolWindowFactory.kt
package cc.unitmesh.devti.gui

import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.sketch.ui.plan.PlanLangSketch
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splittable
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.psi.PsiFile
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.lang.LanguageUtil
import com.intellij.openapi.fileTypes.FileTypeManager
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.Box
import java.util.concurrent.atomic.AtomicBoolean

class AutoDevPlannerToolWindowFactory : ToolWindowFactory, ToolWindowManagerListener, DumbAware {
    private val orientation = AtomicBoolean(true)

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val panel = AutoDevPlanerTooWindow(project)
        val manager = toolWindow.contentManager
        manager.addContent(manager.factory.createContent(panel, null, false).apply { isCloseable = false })
        project.messageBus.connect(manager).subscribe(ToolWindowManagerListener.TOPIC, this)
        toolWindow.setTitleActions(listOfNotNull(ActionUtil.getAction("AutoDevPlanner.ToolWindow.TitleActions")))
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
    override fun getName(): String = "AutoDev Planer"
    var connection = ApplicationManager.getApplication().messageBus.connect(this)
    var content = """
1. 更新数据库表结构
    - [*] 确定 BlogPost 表是否需要新增 category 列（通过 JPA 实体类变更自动生成）
    - [ ] 检查是否存在数据库迁移脚本（当前无 Flyway/Liquibase 痕迹）
    - [ ] 确认是否需要手动执行 ALTER TABLE 语句

2. 更新实体类与 DTO
    - [*] 修改 [BlogPost.java](src/main/java/cc/unitmesh/untitled/demo/entity/BlogPost.java) 添加 category 字段
    - [ ] 更新 [CreateBlogRequest.java](src/main/java/cc/unitmesh/untitled/demo/dto/CreateBlogRequest.java) 包含 category 参数
    - [ ] 验证实体类与 DTO 的字段映射关系

3. 持久层改造
    - [*] 在 [BlogRepository.java](src/main/java/cc/unitmesh/untitled/demo/repository/BlogRepository.java) 添加查询方法
    - [ ] 确定使用派生查询（findByCategory）还是 @Query 注解方式
    - [ ] 检查 MeetingRepository 是否误扩展了 BlogPost 实体（需确认是否设计错误）

4. 业务逻辑层扩展
    - [*] 在 [BlogService.java](src/main/java/cc/unitmesh/untitled/demo/service/BlogService.java) 添加 getBlogsByCategory 方法
    - [ ] 处理空分类/默认分类等边界情况
    - [ ] 验证事务传播特性

5. 接口层开发
    - [*] 在 [BlogController.java](src/main/java/cc/unitmesh/untitled/demo/controller/BlogController.java) 添加新端点
    - [ ] 设计 RESTful 路径（建议 /blogs/category/{category}）
    - [ ] 添加 Swagger 文档注解

6. 数据一致性验证
    - [ ] 检查现有博客数据的 category 字段默认值
    - [ ] 验证更新操作时的 category 字段维护
    - [ ] 确保查询结果排序一致性（添加 @OrderBy 注解）

7. 测试验证
    - [ ] 编写集成测试验证完整流程
    - [ ] 添加 Controller 层单元测试
    - [ ] 验证 SQL 查询性能（通过 EXPLAIN 分析）
"""
    var planLangSketch: PlanLangSketch = PlanLangSketch(project, content, MarkdownPlanParser.parse(content).toMutableList(), true)

    private var markdownEditor: MarkdownLanguageField? = null
    private val contentPanel = JPanel(BorderLayout())
    private var isEditorMode = false
    private var currentCallback: ((String) -> Unit)? = null
    private val planPanel: JPanel by lazy { createPlanPanel() }

    init {
        contentPanel.add(planPanel, BorderLayout.CENTER)
        add(contentPanel, BorderLayout.CENTER)

        connection.subscribe(PlanUpdateListener.TOPIC, object : PlanUpdateListener {
            override fun onPlanUpdate(items: MutableList<AgentTaskEntry>) {
                if (!isEditorMode) {
                    planLangSketch.updatePlan(items)
                }
            }
        })
    }

    private fun createPlanPanel(): JPanel {
        return panel {
            row {
                cell(planLangSketch)
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
    }

    private fun switchToEditorView() {
        if (isEditorMode) return

        if (markdownEditor == null) {
            markdownEditor = MarkdownLanguageField(project, content, "Edit your plan here...", "plan.md")
        } else {
            markdownEditor?.text = content
        }

        val buttonPanel = JPanel(BorderLayout())
        val buttonsBox = Box.createHorizontalBox().apply {
            add(JButton("Save").apply {
                addActionListener {
                    val newContent = markdownEditor?.text ?: ""
                    switchToPlanView(newContent)
                    currentCallback?.invoke(newContent)
                }
            })
            add(Box.createHorizontalStrut(10))
            add(JButton("Cancel").apply {
                addActionListener {
                    switchToPlanView()
                }
            })
        }
        buttonPanel.add(buttonsBox, BorderLayout.EAST)
        buttonPanel.border = JBUI.Borders.empty(5)

        contentPanel.removeAll()
        val editorPanel = JPanel(BorderLayout())
        editorPanel.add(JBScrollPane(markdownEditor), BorderLayout.CENTER)
        editorPanel.add(buttonPanel, BorderLayout.SOUTH)

        contentPanel.add(editorPanel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()

        isEditorMode = true
    }

    private fun switchToPlanView(newContent: String? = null) {
        if (newContent != null && newContent != content) {
            content = newContent

            val parsedItems = MarkdownPlanParser.parse(newContent).toMutableList()
            planLangSketch.updatePlan(parsedItems)
        }

        contentPanel.removeAll()
        contentPanel.add(planPanel, BorderLayout.CENTER)
        contentPanel.revalidate()
        contentPanel.repaint()

        isEditorMode = false
    }

    override fun dispose() {
        markdownEditor = null
    }

    companion object {
        fun showPlanEditor(project: Project, planText: String, callback: (String) -> Unit) {
            val toolWindow =
                ToolWindowManager.getInstance(project).getToolWindow(AutoDevPlannerToolWindowFactory.PlANNER_ID)
            if (toolWindow != null) {
                val content = toolWindow.contentManager.getContent(0)
                val plannerWindow = content?.component as? AutoDevPlanerTooWindow

                plannerWindow?.let {
                    it.currentCallback = callback
                    if (planText.isNotEmpty() && planText != it.content) {
                        it.content = planText
                    }

                    it.switchToEditorView()
                    toolWindow.show()
                }
            }
        }
    }
}

private class MarkdownLanguageField(
    private val myProject: Project?,
    val value: String,
    private val placeholder: String,
    private val fileName: String
) : LanguageTextField(
    LanguageUtil.getFileTypeLanguage(FileTypeManager.getInstance().getFileTypeByExtension("md")), myProject, value,
    object : SimpleDocumentCreator() {
        override fun createDocument(value: String?, language: Language?, project: Project?): Document {
            return createDocument(value, language, project, this)
        }

        override fun customizePsiFile(file: PsiFile?) {
            file?.name = fileName
        }
    }
) {
    override fun createEditor(): EditorEx {
        return super.createEditor().apply {
            setShowPlaceholderWhenFocused(true)
            setHorizontalScrollbarVisible(true)
            setVerticalScrollbarVisible(true)
            setPlaceholder(placeholder)

            val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
            this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)

            settings.isLineNumbersShown = true
            settings.isLineMarkerAreaShown = false
            settings.isFoldingOutlineShown = false
            settings.isUseSoftWraps = true
        }
    }
}

