package cc.unitmesh.devti.language.config

import cc.unitmesh.devti.language.console.addCancelCallback
import cc.unitmesh.devti.language.run.runner.PostFunction
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import java.util.concurrent.CompletableFuture

/**
 * @author lk
 */
abstract class ShireInteractionTask(project: Project, taskName: String, val postExecute: PostFunction?): Backgroundable(project, taskName) {

    /**
     * An unexpected exception occurred, causing the shire process cannot be canceled,
     * postExecute was not executed,it may have used the [CompletableFuture].
     */
    override fun onThrowable(error: Throwable) {
        super.onThrowable(error)
        postExecute?.invoke(null, null)
    }

}

fun ShireInteractionTask.cancelWithConsole(consoleView: ConsoleView?): ShireInteractionTask =
    apply { consoleView?.addCancelCallback { onCancel() } }