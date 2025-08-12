package cc.unitmesh.diagram.graphviz

import cc.unitmesh.diagram.graphviz.editor.GraphvizPreviewFileEditor
import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramBuilderFactory
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.graph.services.GraphLayoutService
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.uml.components.UmlGraphZoomableViewport
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Panel for displaying Graphviz diagrams
 * Similar to JdlDiagramPanel in JHipster UML implementation
 */
class GraphvizDiagramPanel(private val fileEditor: GraphvizPreviewFileEditor) : Disposable {
    private var builder: DiagramBuilder? = null
    private val chartPanel = MyPanel()
    private val umlProvider = GraphvizUmlProvider()

    override fun dispose() {}

    fun getComponent(): JComponent {
        return chartPanel
    }

    fun draw() {
        if (builder == null) {
            val project = fileEditor.getProject()
            val virtualFile = fileEditor.getFile()

            val rootData = GraphvizElementManager.getRootData(project, virtualFile)

            builder = DiagramBuilderFactory.getInstance()
                .create(project, umlProvider, rootData, null)

            Disposer.register(this, builder!!)
            builder!!.view.fitContentOnResize = true

            val graphView = createSimpleGraphView(builder!!)
            chartPanel.add(graphView, BorderLayout.CENTER)

            val actionsProvider = builder!!.provider.getExtras().toolbarActionsProvider
            val actionGroup = actionsProvider.createToolbarActions(builder!!)
            val actionToolbar = ActionManager.getInstance().createActionToolbar("AUTODEV.DOT", actionGroup, true)
            actionToolbar.targetComponent = graphView
            actionToolbar.component.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0))

            chartPanel.add(actionToolbar.component, BorderLayout.NORTH)

            builder!!.queryUpdate()
                .withDataReload()
                .withPresentationUpdate()
                .withRelayout()
                .runAsync()
        }
    }

    private fun createSimpleGraphView(builder: DiagramBuilder): JComponent {
        builder.presentationModel.registerActions()
        val view = builder.view
        view.canvasComponent.background = JBColor.GRAY

        GraphLayoutService.getInstance()
            .queryLayout(builder.graphBuilder)
            .withFitContent(GraphLayoutService.GraphLayoutQueryParams.FitContentOption.AFTER)
            .run()

        return UmlGraphZoomableViewport(builder)
    }
    
    private inner class MyPanel : JPanel(BorderLayout()) {
        init {
            DataManager.registerDataProvider(this) { dataId ->
                when {
                    PlatformCoreDataKeys.BGT_DATA_PROVIDER.`is`(dataId) -> DataProvider { slowId ->
                        when {
                            CommonDataKeys.PSI_FILE.`is`(slowId) ->
                                PsiManager.getInstance(fileEditor.getProject())
                                    .findFile(fileEditor.getFile())
                            else -> null
                        }
                    }
                    CommonDataKeys.VIRTUAL_FILE.`is`(dataId) -> fileEditor.getFile()
                    else -> null
                }
            }
        }
    }
}


