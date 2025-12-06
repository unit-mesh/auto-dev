package cc.unitmesh.devins.ui.nano

import cc.unitmesh.xuiper.action.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.swing.JOptionPane

/**
 * Compose/Desktop implementation of NanoActionHandler
 * 
 * Handles NanoUI actions in a Compose Desktop environment.
 * Provides platform-specific implementations for navigation, toast, and fetch.
 * 
 * Example:
 * ```kotlin
 * val handler = ComposeActionHandler(
 *     scope = rememberCoroutineScope(),
 *     onNavigate = { route -> navController.navigate(route) },
 *     onToast = { message -> snackbarHostState.showSnackbar(message) }
 * )
 * 
 * handler.registerCustomAction("AddTask") { payload, context ->
 *     val title = payload["title"] as? String ?: ""
 *     taskRepository.add(Task(title))
 *     ActionResult.Success
 * }
 * ```
 */
class ComposeActionHandler(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val onNavigate: ((String) -> Unit)? = null,
    private val onToast: ((String) -> Unit)? = null,
    private val onFetchComplete: ((String, Boolean, String?) -> Unit)? = null
) : BaseNanoActionHandler() {
    
    private val httpClient = HttpClient.newBuilder().build()
    
    override fun handleNavigate(
        navigate: NanoAction.Navigate,
        context: NanoActionContext
    ): ActionResult {
        return try {
            if (onNavigate != null) {
                onNavigate.invoke(navigate.to)
            } else {
                // Default: open in browser if it's a URL
                if (navigate.to.startsWith("http://") || navigate.to.startsWith("https://")) {
                    Desktop.getDesktop().browse(URI(navigate.to))
                }
            }
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Error("Navigation failed: ${e.message}", e)
        }
    }
    
    override fun handleFetch(
        fetch: NanoAction.Fetch,
        context: NanoActionContext
    ): ActionResult {
        // Set loading state if specified
        fetch.loadingState?.let { path ->
            context.set(path, true)
        }
        
        scope.launch {
            try {
                val requestBuilder = HttpRequest.newBuilder()
                    .uri(URI(fetch.url))
                
                // Set method
                when (fetch.method) {
                    HttpMethod.GET -> requestBuilder.GET()
                    HttpMethod.POST -> {
                        val body = buildRequestBody(fetch.body, context)
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body))
                        requestBuilder.header("Content-Type", fetch.contentType.mimeType)
                    }
                    HttpMethod.PUT -> {
                        val body = buildRequestBody(fetch.body, context)
                        requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body))
                        requestBuilder.header("Content-Type", fetch.contentType.mimeType)
                    }
                    HttpMethod.DELETE -> requestBuilder.DELETE()
                    else -> requestBuilder.GET()
                }
                
                // Add headers
                fetch.headers?.forEach { (key, value) ->
                    requestBuilder.header(key, value)
                }
                
                val response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
                )
                
                // Update loading state
                fetch.loadingState?.let { path ->
                    context.set(path, false)
                }
                
                if (response.statusCode() in 200..299) {
                    // Success
                    fetch.responseBinding?.let { path ->
                        context.set(path, response.body())
                    }
                    
                    fetch.onSuccess?.let { successAction ->
                        handleAction(successAction, context)
                    }
                    
                    onFetchComplete?.invoke(fetch.url, true, response.body())
                } else {
                    // Error
                    val errorMsg = "HTTP ${response.statusCode()}: ${response.body()}"
                    fetch.errorBinding?.let { path ->
                        context.set(path, errorMsg)
                    }
                    
                    fetch.onError?.let { errorAction ->
                        handleAction(errorAction, context)
                    }
                    
                    onFetchComplete?.invoke(fetch.url, false, errorMsg)
                }
                
            } catch (e: Exception) {
                fetch.loadingState?.let { path ->
                    context.set(path, false)
                }
                fetch.errorBinding?.let { path ->
                    context.set(path, e.message)
                }
                fetch.onError?.let { errorAction ->
                    handleAction(errorAction, context)
                }
                onFetchComplete?.invoke(fetch.url, false, e.message)
            }
        }
        
        return ActionResult.Pending { /* async operation */ }
    }
    
    override fun handleShowToast(
        toast: NanoAction.ShowToast,
        context: NanoActionContext
    ): ActionResult {
        return try {
            if (onToast != null) {
                onToast.invoke(toast.message)
            } else {
                // Default: use Swing dialog (for desktop)
                JOptionPane.showMessageDialog(null, toast.message)
            }
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Error("Toast failed: ${e.message}", e)
        }
    }
    
    private fun buildRequestBody(
        body: Map<String, BodyField>?,
        context: NanoActionContext
    ): String {
        if (body == null) return ""
        
        val resolvedBody = body.mapValues { (_, field) ->
            when (field) {
                is BodyField.Literal -> field.value
                is BodyField.StateBinding -> context.get(field.path)?.toString() ?: ""
            }
        }
        
        // Simple JSON serialization
        return buildString {
            append("{")
            resolvedBody.entries.forEachIndexed { index, (key, value) ->
                if (index > 0) append(",")
                append("\"$key\":\"$value\"")
            }
            append("}")
        }
    }
}

