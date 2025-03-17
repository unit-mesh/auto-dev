// filepath: /Volumes/source/ai/autocrud/core/src/main/kotlin/cc/unitmesh/devti/gui/AutoDevPlannerToolWindowFactory.kt
package cc.unitmesh.devti.gui

import cc.unitmesh.devti.inline.fullWidth
import cc.unitmesh.devti.observer.plan.AgentTaskEntry
import cc.unitmesh.devti.observer.plan.MarkdownPlanParser
import cc.unitmesh.devti.observer.plan.PlanUpdateListener
import cc.unitmesh.devti.sketch.ui.plan.PlanSketch
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Splittable
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsActions
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
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.PsiFile
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.lang.LanguageUtil
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FontMetrics
import javax.swing.JComponent
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
    override fun getName(): @NlsActions.ActionText String? = "AutoDev Planer"
    var connection = ApplicationManager.getApplication().messageBus.connect(this)

    val content = """1. 分析当前Blog功能结构（✓）
   - 当前Blog功能分散在entity(BlogPost)、service、controller层，采用贫血模型
   - domain.Blog类存在但未充分使用，需要明确领域模型边界

2. 建立领域模型（*）
   a. 定义Blog聚合根（进行中）
   b. 创建Value Object（标题、内容等）
   c. 定义领域服务接口
   d. 实现业务规则（如发布校验、状态转换）

3. 重构数据持久层
   - 将BlogRepository改为面向领域模型
   - 移除BlogPost实体与数据库的直接映射

4. 调整应用层
   - 重写BlogService使用领域模型
   - 修改BlogController适配DTO转换

5. 业务逻辑迁移
   - 将Service中的CRUD逻辑转移到Blog领域对象
   - 实现领域事件机制（如博客发布事件）

6. 验证测试
   - 修改现有测试用例
   - 添加领域模型单元测试"""
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

    companion object {
        fun showPlanEditor(project: Project, planText: String, callback: (String) -> Unit) {
            val dialog = object : DialogWrapper(project) {
                private val markdownEditor =
                    MarkdownLanguageField(project, planText, "Edit your plan here...", "plan.md")

                init {
                    title = "Edit Plan"
                    // Ensure the text is properly set
                    if (markdownEditor.text.isEmpty() && planText.isNotEmpty()) {
                        markdownEditor.text = planText
                    }
                    init()
                }

                override fun createCenterPanel(): JComponent {
                    val panel = JBScrollPane(markdownEditor)
                    panel.preferredSize = Dimension(800, 600)
                    return panel
                }

                override fun doOKAction() {
                    super.doOKAction()
                    callback(markdownEditor.text)
                }
            }

            dialog.show()
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

            val metrics: FontMetrics = getFontMetrics(font)
            val columnWidth = metrics.charWidth('m')
            isOneLineMode = false
            preferredSize = Dimension(50 * columnWidth, 30 * metrics.height)

            settings.isLineNumbersShown = true
            settings.isLineMarkerAreaShown = true
            settings.isFoldingOutlineShown = true
        }
    }
}

