package cc.unitmesh.devti.language.middleware.builtin.psi

import cc.unitmesh.devti.provider.RunService
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.util.messages.MessageBusConnection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object PsiErrorCollector {
    fun collectSyntaxError(
        psiFile: PsiFile,
        project: Project,
    ): List<String> {
        var errors: List<String> = listOf()
        collectSyntaxError(psiFile, psiFile.virtualFile, project) {
            errors = it
        }

        return errors
    }

    /**
     * This function is used to collect syntax errors in a given source file using the PSI (Program Structure Interface) of the file.
     * It takes the source file, a callback function to run after collecting errors, an output file, and the project as parameters.
     *
     * @param sourceFile The PSI file from which syntax errors need to be collected.
     * @param runAction A callback function that takes a list of errors as input and performs some action.
     * @param outputFile The virtual file where the errors will be collected.
     * @param project The project to which the files belong.
     */
    fun collectSyntaxError(
        sourceFile: PsiFile,
        outputFile: VirtualFile,
        project: Project,
        runAction: ((errors: List<String>) -> Unit)?,
    ) {
        val collectPsiError = sourceFile.collectPsiError()
        if (collectPsiError.isNotEmpty()) {
            return runAction?.invoke(collectPsiError) ?: Unit
        }

        val document = runReadAction { FileDocumentManager.getInstance().getDocument(outputFile) } ?: return

        val range = TextRange(0, document.textLength)
        val errors = mutableListOf<String>()

        DaemonCodeAnalyzerEx.getInstance(project).restart(sourceFile)

        val hintDisposable = Disposer.newDisposable()
        val busConnection: MessageBusConnection = project.messageBus.connect(hintDisposable)
        val future: CompletableFuture<List<String>> = CompletableFuture()
        busConnection.subscribe(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            SimpleCodeErrorListener(document, project, range, errors, busConnection, hintDisposable) {
                future.complete(it)
            }
        )

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
                HighlightSeverity.ERROR,
                range.startOffset,
                range.endOffset
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
    val visitor = object : RunService.PsiSyntaxCheckingVisitor() {
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
