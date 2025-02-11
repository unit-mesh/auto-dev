package cc.unitmesh.devti.sketch.lint

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ex.GlobalInspectionContextBase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.PairProcessor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

data class SketchInspectionError(
    val lineNumber: Int,
    val description: String,
    val highlightType: ProblemHighlightType,
) {
    companion object {
        fun from(problemDescriptor: ProblemDescriptor): SketchInspectionError {
            return SketchInspectionError(
                problemDescriptor.lineNumber,
                problemDescriptor.descriptionTemplate,
                problemDescriptor.highlightType
            )
        }
    }
}

object SketchCodeInspection {
    fun showErrors(errors: List<SketchInspectionError>, panel: JPanel) {
        val columnNames = arrayOf("Line", "Description", "Highlight Type")
        val data = errors.map {
            arrayOf(it.lineNumber, it.description, it.highlightType.toString())
        }.toTypedArray()

        val tableModel = DefaultTableModel(data, columnNames)

        val table = JBTable(tableModel).apply {
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        }

        val scrollPane = JBScrollPane(table).apply {
            preferredSize = Dimension(480, 400)
        }
        val errorLabel = JBLabel(AutoDevBundle.message("sketch.lint.error", errors.size)).apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        errorLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                createPopup(scrollPane, table, errors).showInCenterOf(panel)
            }
            override fun mouseEntered(e: MouseEvent) {
                errorLabel.toolTipText = AutoDevBundle.message("sketch.lint.error.tooltip")
            }
        })

        val errorPanel = JPanel().apply {
            background = JBColor.WHITE
            layout = FlowLayout(FlowLayout.LEFT)
            add(errorLabel)
        }

        panel.add(errorPanel)
    }

    private fun createPopup(
        scrollPane: JBScrollPane,
        table: JBTable,
        errors: List<SketchInspectionError>
    ): JBPopup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(scrollPane, table)
        .setTitle("Found Lint Issues: ${errors.size}")
        .setResizable(true)
        .setMovable(true)
        .setRequestFocus(true)
        .createPopup()

    fun runInspections(project: Project, psiFile: PsiFile, originFile: VirtualFile): List<SketchInspectionError> {
        val globalContext = InspectionManager.getInstance(project).createNewGlobalContext()
                as? GlobalInspectionContextBase ?: return emptyList()

        val originPsi = runReadAction { PsiManager.getInstance(project).findFile(originFile) }
            ?: return emptyList()

        globalContext.currentScope = AnalysisScope(originPsi)

        val toolsCopy = collectTools(project, psiFile, globalContext)
        if (toolsCopy.isEmpty()) {
            return emptyList()
        }

        return runReadAction {
            val indicator = DaemonProgressIndicator()
            val result: Map<LocalInspectionToolWrapper, List<ProblemDescriptor>> = InspectionEngine.inspectEx(
                toolsCopy, psiFile, psiFile.textRange, psiFile.textRange, false, false, true,
                indicator, PairProcessor.alwaysTrue<LocalInspectionToolWrapper?, ProblemDescriptor?>()
            )

            val problems = result.values.flatten()
            return@runReadAction problems
                .sortedBy { it.lineNumber }
                .distinctBy { it.lineNumber }.map {
                    SketchInspectionError.from(it)
                }
        }
    }

    private fun collectTools(
        project: Project,
        psiFile: PsiFile,
        globalContext: GlobalInspectionContextBase
    ): MutableList<LocalInspectionToolWrapper> {
        val inspectionProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
        val toolWrappers = inspectionProfile.getInspectionTools(psiFile)
            .filter {
                it.isApplicable(psiFile.language) && it.defaultLevel.severity == HighlightSeverity.ERROR
            }

        toolWrappers.forEach {
            it.initialize(globalContext)
        }

        val toolsCopy: MutableList<LocalInspectionToolWrapper> = ArrayList<LocalInspectionToolWrapper>(toolWrappers.size)
        for (tool in toolWrappers) {
            if (tool is LocalInspectionToolWrapper) {
                toolsCopy.add(tool.createCopy())
            }
        }

        return toolsCopy
    }
}