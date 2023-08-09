package cc.unitmesh.devti

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

@Service(Service.Level.PROJECT)
class LLMCoroutineScope(val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)) {
    companion object {
        fun scope(project: Project): CoroutineScope = project.service<LLMCoroutineScope>().coroutineScope
    }
}