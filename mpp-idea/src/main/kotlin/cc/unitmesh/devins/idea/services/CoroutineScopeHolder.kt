package cc.unitmesh.devins.idea.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.cancel

/**
 * A service-level class that provides and manages coroutine scopes for a given project.
 * Implements [Disposable] to properly cancel all coroutines when the project is closed.
 *
 * @constructor Initializes the [CoroutineScopeHolder] with a project instance.
 * @param project The project this service is associated with.
 */
@Service(Level.PROJECT)
class CoroutineScopeHolder(private val project: Project) : Disposable {

    private val parentJob = SupervisorJob()
    private val projectWideCoroutineScope: CoroutineScope = CoroutineScope(parentJob + Dispatchers.Default)

    /**
     * Creates a new coroutine scope as a child of the project-wide coroutine scope with the specified name.
     *
     * @param name The name for the newly created coroutine scope.
     * @return a scope with a Job which parent is the Job of projectWideCoroutineScope scope.
     *
     * The returned scope can be completed only by cancellation.
     * projectWideCoroutineScope scope will cancel the returned scope when canceled.
     * If the child scope has a narrower lifecycle than projectWideCoroutineScope scope,
     * then it should be canceled explicitly when not needed,
     * otherwise, it will continue to live in the Job hierarchy until termination of the CoroutineScopeHolder service.
     */
    fun createScope(name: String): CoroutineScope {
        val childJob = SupervisorJob(parentJob)
        return CoroutineScope(childJob + Dispatchers.Default + CoroutineName(name))
    }

    /**
     * Cancels all coroutines when the project is disposed.
     */
    override fun dispose() {
        parentJob.cancel()
    }
}

