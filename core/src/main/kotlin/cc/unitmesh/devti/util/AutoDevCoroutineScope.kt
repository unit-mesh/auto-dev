package cc.unitmesh.devti.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.*

public val workerThread = AppExecutorUtil.getAppExecutorService().asCoroutineDispatcher()

@Service(Service.Level.APP)
class AutoDevAppScope: Disposable {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger<AutoDevAppScope>().error(throwable)
    }
    val coroutineScope = CoroutineScope(SupervisorJob() + coroutineExceptionHandler)
    val workerScope = CoroutineScope(SupervisorJob() + workerThread)

    override fun dispose() {
        coroutineScope.cancel()
        workerScope.cancel()
    }

    companion object {
        fun scope(): CoroutineScope = service<AutoDevAppScope>().coroutineScope
        fun workerScope(): CoroutineScope = service<AutoDevAppScope>().workerScope
    }
}

@Service(Service.Level.PROJECT)
class AutoDevCoroutineScope : Disposable {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger<AutoDevCoroutineScope>().error(throwable)
    }

    val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + coroutineExceptionHandler)
    val workerScope: CoroutineScope = CoroutineScope(SupervisorJob() + workerThread)

    override fun dispose() {
        coroutineScope.cancel()
        workerScope.cancel()
    }

    companion object {
        fun scope(project: Project): CoroutineScope = project.service<AutoDevCoroutineScope>().coroutineScope

        @Deprecated(
            message = "using this may cause memory leak after project close",
            replaceWith = ReplaceWith("AutoDevCoroutineScope.workScope(project)", imports = ["cc.unitmesh.devti.util.AutoDevCoroutineScope"])
        )
        fun workerThread(): CoroutineScope = CoroutineScope(SupervisorJob() + workerThread)
        fun workerScope(project: Project): CoroutineScope = project.service<AutoDevCoroutineScope>().coroutineScope
    }
}
