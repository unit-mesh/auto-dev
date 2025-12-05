package cc.unitmesh.devti.language.console

import com.intellij.execution.console.ConsoleViewWrapperBase
import com.intellij.execution.ui.ConsoleView

/**
 * This class provides cancel callbacks for the console view
 * and stop the tasks related to it when it is closed,
 *
 * @author lk
 */
open class DevInConsoleViewBase(executionConsole: ConsoleView) : ConsoleViewWrapperBase(executionConsole) {

    open fun cancelCallback(callback: (String) -> Unit) = Unit

    open fun isCanceled() = false

}

fun ConsoleView.addCancelCallback(callback: (String) -> Unit) {
    if (this is DevInConsoleViewBase) {
        cancelCallback(callback)
    }
}

fun ConsoleView.isCanceled(): Boolean {
    if (this is DevInConsoleViewBase) {
        return isCanceled()
    }
    return false
}

