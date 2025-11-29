package cc.unitmesh.devins.idea.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope

/**
 * A service-level class that provides and manages coroutine scopes for a given project.
 *
 * @constructor Initializes the [CoroutineScopeHolder] with a project-wide coroutine scope.
 * @param projectWideCoroutineScope A [CoroutineScope] defining the lifecycle of project-wide coroutines.
 */
@Service(Level.PROJECT)
class CoroutineScopeHolder(private val projectWideCoroutineScope: CoroutineScope) {
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
    @Suppress("UnstableApiUsage")
    fun createScope(name: String): CoroutineScope = projectWideCoroutineScope.childScope(name)
}

