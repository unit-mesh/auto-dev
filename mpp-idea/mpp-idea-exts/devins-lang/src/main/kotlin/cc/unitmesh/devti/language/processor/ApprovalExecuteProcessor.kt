package cc.unitmesh.devti.language.processor

import cc.unitmesh.devti.language.ast.action.PatternActionFuncDef
import cc.unitmesh.devti.language.ast.action.PatternProcessor
import com.intellij.ide.DataManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import cc.unitmesh.devti.language.processor.ui.PendingApprovalPanel
import java.util.concurrent.CompletableFuture

object ApprovalExecuteProcessor: PatternProcessor {
    override val type: PatternActionFuncDef = PatternActionFuncDef.APPROVAL_EXECUTE

    fun execute(
        myProject: Project,
        filename: Any,
        variableNames: Array<String>,
        variableTable: MutableMap<String, Any?>,
        approve: ((Any) -> Unit)? = null,
        reject: (() -> Unit?)? = null
    ): Any {
        val dataContext = DataManager.getInstance().dataContextFromFocusAsync.blockingGet(10000)
            ?: throw IllegalStateException("No data context")

        val panel = PendingApprovalPanel()

        val future = CompletableFuture<Any>()

        runInEdt {
            val popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, null)
                .setResizable(true)
                .setMovable(true)
                .setFocusable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(false)
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .setKeyboardActions(listOf())
                .createPopup()

            panel.setupKeyShortcuts(popup,
                {
                    popup.closeOk(null)
                    approve?.invoke("")
                    future.complete("")
                },
                {
                    popup.cancel()
                    reject?.invoke()
                    future.complete("")
                })

            popup.showInBestPositionFor(dataContext)
        }

        return future.get()
    }
}

