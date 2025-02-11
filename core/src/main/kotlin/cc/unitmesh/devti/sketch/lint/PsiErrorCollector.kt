package cc.unitmesh.devti.sketch.lint

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ex.GlobalInspectionContextBase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.PairProcessor
import com.intellij.util.messages.MessageBusConnection
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
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

object PsiErrorUI {
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
            preferredSize = Dimension(600, 300)
        }
        val errorLabel = JBLabel("Found Lint issue: ${errors.size}").apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        errorLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                createErrorButton(scrollPane, table, errors).showInCenterOf(panel)
            }

            // hover also show tip
            override fun mouseEntered(e: MouseEvent) {
                errorLabel.toolTipText = "Click to view all errors"
            }
        })

        panel.add(errorLabel)
    }

    private fun createErrorButton(
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
}

object PsiErrorCollector {
    fun runInspections(project: Project, psiFile: PsiFile, originFile: VirtualFile): List<SketchInspectionError> {
        val globalContext = InspectionManager.getInstance(project).createNewGlobalContext()
                as? GlobalInspectionContextBase ?: return emptyList()

        val originPsi = runReadAction { PsiManager.getInstance(project).findFile(originFile) }
            ?: return emptyList()

        globalContext.currentScope = AnalysisScope(originPsi)

        val toolsCopy = localInspectionToolWrappers(project, psiFile, globalContext)

        if (toolsCopy.isEmpty()) {
            return emptyList()
        }

        val indicator = DaemonProgressIndicator()
        return runReadAction {
            val result: Map<LocalInspectionToolWrapper, List<ProblemDescriptor>> = InspectionEngine.inspectEx(
                toolsCopy, psiFile, psiFile.textRange, psiFile.textRange, false, false, true,
                indicator, PairProcessor.alwaysTrue<LocalInspectionToolWrapper?, ProblemDescriptor?>()
            )

            val problems = result.values.flatten()
            return@runReadAction problems.sortedBy { it.lineNumber }.distinctBy { it.lineNumber }.map {
                SketchInspectionError.from(it)
            }
        }
    }

    private val frontEndSkipError =
        setOf(
            "NpmUsedModulesInstalled",
            "ProblematicWhitespace",
            "JSXUnresolvedComponent",
            "UnterminatedStatement",
            "UnterminatedStatementJS"
        )

    private fun localInspectionToolWrappers(
        project: Project,
        psiFile: PsiFile,
        globalContext: GlobalInspectionContextBase
    ): MutableList<LocalInspectionToolWrapper> {
        val inspectionProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
        val toolWrappers = inspectionProfile.getInspectionTools(psiFile)
            .filter {
                it.isApplicable(psiFile.language)
                        && it.displayKey?.id !in frontEndSkipError
                        && it.defaultLevel.severity == HighlightSeverity.WARNING || it.defaultLevel.severity == HighlightSeverity.ERROR
            }

        toolWrappers.forEach {
            it.initialize(globalContext)
        }

        val toolsCopy: MutableList<LocalInspectionToolWrapper> =
            ArrayList<LocalInspectionToolWrapper>(toolWrappers.size)
        for (tool in toolWrappers) {
            if (tool is LocalInspectionToolWrapper) {
                toolsCopy.add(tool.createCopy())
            }
        }
        return toolsCopy
    }

    /**
     * This function is used to collect syntax errors in a given source file using the PSI (Program Structure Interface) of the file.
     * It takes the source file, a callback function to run after collecting errors, an output file, and the project as parameters.
     *
     * @param sourceFile The PSI file from which syntax errors need to be collected.
     * @param runAction A callback function that takes a list of errors as input and performs some action.
     * @param project The project to which the files belong.
     */
    fun collectSyntaxError(
        sourceFile: PsiFile,
        project: Project,
        runAction: ((errors: List<String>) -> Unit)?,
    ) {
        val collectPsiError = sourceFile.collectPsiError()
        if (collectPsiError.isNotEmpty()) {
            return runAction?.invoke(collectPsiError) ?: Unit
        }

        val document = runReadAction { FileDocumentManager.getInstance().getDocument(sourceFile.virtualFile) } ?: return

        val range = TextRange(0, document.textLength)
        val errors = mutableListOf<String>()

        val hintDisposable = Disposer.newDisposable()
        val busConnection: MessageBusConnection = project.messageBus.connect(hintDisposable)
        val future: CompletableFuture<List<String>> = CompletableFuture()
        busConnection.subscribe(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            SimpleCodeErrorListener(document, project, range, errors, busConnection, hintDisposable) {
                future.complete(it)
            }
        )

        runReadAction {
            DaemonCodeAnalyzerEx.getInstanceEx(project).restart(sourceFile)
        }

        future.get(30, TimeUnit.SECONDS)
    }

    class SimpleCodeErrorListener(
        private val document: Document,
        private val project: Project,
        private val range: TextRange,
        private val errors: MutableList<String>,
        private val busConnection: MessageBusConnection,
        private val hintDisposable: Disposable,
        private val runAction: ((errors: List<String>) -> Unit)?,
    ) : DaemonCodeAnalyzer.DaemonListener {
        override fun daemonFinished() {
            DaemonCodeAnalyzerEx.processHighlights(
                document,
                project,
                HighlightSeverity.WARNING,
                range.startOffset,
                range.endOffset // todo: modify to patch part only
            ) {
                if (it.description != null) {
                    errors.add(it.description)
                }

                true
            }

            runAction?.invoke(errors)
            busConnection.disconnect()
            Disposer.dispose(hintDisposable)
        }
    }
}

/**
 * This function is an extension function for PsiFile class in Kotlin.
 * It collects syntax errors present in the PsiFile and returns a list of error messages.
 * It creates a PsiSyntaxCheckingVisitor object to visit each element in the PsiFile.
 * If the element is a PsiErrorElement, it adds a message to the errors list with the error description and position.
 * Finally, it returns the list of error messages.
 */
fun PsiFile.collectPsiError(): MutableList<String> {
    val errors = mutableListOf<String>()
    val visitor = object : PsiSyntaxCheckingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is PsiErrorElement) {
                errors.add("Syntax error at position ${element.textRange.startOffset}: ${element.errorDescription}")
            }
            super.visitElement(element)
        }
    }

    this.accept(visitor)
    return errors
}
