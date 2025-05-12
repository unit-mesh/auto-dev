package cc.unitmesh.devti.language.compiler.streaming

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.language.console.DevInConsoleViewBase
import cc.unitmesh.devti.devins.post.LifecycleProcessorSignature
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project


/**
 * The OnStreamingService class is responsible for managing all the [StreamingServiceProvider] instances related to streaming services.
 * It offers methods for registering, clearing, and initiating streaming services within the application.
 *
 * This class is annotated with the @Service annotation at the project level, indicating its role in the service management infrastructure.
 *
 * The class maintains a mutable map to associate [LifecycleProcessorSignature] objects with corresponding [StreamingServiceProvider] instances.
 * It also holds an optional reference to a console view object that can be used for outputting information to the user.
 */
@Service(Service.Level.PROJECT)
class OnStreamingService {
    val map = mutableMapOf<LifecycleProcessorSignature, StreamingServiceProvider>()
    var console: DevInConsoleViewBase? = null

    fun registerStreamingService(sign: LifecycleProcessorSignature, console: DevInConsoleViewBase?) {
        this.console = console
        val streamingService = StreamingServiceProvider.Companion.getStreamingService(sign.funcName)
        if (streamingService != null) {
            map[sign] = streamingService
            streamingService.onCreated(console)
        }
    }

    fun clearStreamingService() {
        map.clear()
    }

    fun all(): List<StreamingServiceProvider> {
        return StreamingServiceProvider.Companion.all()
    }

    fun onStart(project: Project, userPrompt: String) {
        map.forEach { (_, service) ->
            try {
                service.onBeforeStreaming(project, userPrompt, console)
            } catch (e: Exception) {
                AutoDevNotifications.error(project, "Error on start streaming service: ${e.message}")
            }
        }
    }

    fun onStreaming(project: Project, chunk: String) {
        map.forEach { (sign, service) ->
            try {
                service.
                onStreaming(project, chunk, sign.args)
            } catch (e: Exception) {
                AutoDevNotifications.error(project, "Error on streaming service: ${e.message}")
            }
        }
    }

    fun onDone(project: Project) {
        map.forEach { (_, service) ->
            try {
                service.afterStreamingDone(project)
            } catch (e: Exception) {
                AutoDevNotifications.error(project, "Error on done streaming service: ${e.message}")
            }
        }
    }

    fun onStreamingError() {
        // todo
    }
}
