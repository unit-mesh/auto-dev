package cc.unitmesh.devti.llms

import com.intellij.execution.ui.ConsoleView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.cancellable

/**
 * This class supports cancel callback for [Flow],
 * the flow consumer cancels the flow collection after callback is called,
 * which can be used to skip processing useless data.
 *
 * @author lk
 */
class CustomFlowWrapper<T>(private val delegate: Flow<T>) : Flow<T> {

    private var isCanceled = false

    private var _cancelCallback: ((String) -> Unit)? = null

    override suspend fun collect(collector: FlowCollector<T>) {
        if (!isCanceled) {
            delegate.collect(collector)
        }
    }

    fun cancelCallback(callback: (String) -> Unit) {
        _cancelCallback = callback
    }

    fun cancel(message: String) {
        check(isCanceled == false) { "This flow has been canceled" }
        isCanceled = true
        _cancelCallback?.invoke(message)
    }
}

/**
 * Please use [CustomFlowWrapper] to call it, and it won't work if it's CancellableFlow,
 * so you need to call it before calling [cancellable]
 */
fun <T> Flow<T>.cancelHandler(handle: ((String) -> Unit) -> Unit): Flow<T> =
    apply { if (this is CustomFlowWrapper<T>) handle({ cancel(it) }) }
