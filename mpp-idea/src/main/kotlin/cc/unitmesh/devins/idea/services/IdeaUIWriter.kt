package cc.unitmesh.devins.idea.services

import com.intellij.openapi.application.ApplicationManager
import java.io.StringWriter
import java.io.Writer

/**
 * A Writer implementation that updates UI components based on the output stream.
 * Uses callbacks to abstract UI interactions.
 * 
 * Adapted from UIUpdatingWriter in core module for use with Compose UI.
 */
class IdeaUIWriter(
    private val onTextUpdate: (String, Boolean) -> Unit,
    private val onStateUpdate: (IdeaTerminalExecutionState, String?) -> Unit
) : Writer() {
    private val stringWriter = StringWriter()
    private var isExecuting = false

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        stringWriter.write(cbuf, off, len)
        updateUI()
    }

    override fun flush() {
        stringWriter.flush()
        updateUI()
    }

    override fun close() {
        stringWriter.close()
        isExecuting = false
        updateUI()
    }

    fun setExecuting(executing: Boolean) {
        isExecuting = executing
        if (executing) {
            onStateUpdate(IdeaTerminalExecutionState.EXECUTING, null)
        }
        updateUI()
    }

    fun setSuccess() {
        isExecuting = false
        onStateUpdate(IdeaTerminalExecutionState.SUCCESS, null)
        updateUI()
    }

    fun setFailed(message: String?) {
        isExecuting = false
        onStateUpdate(IdeaTerminalExecutionState.FAILED, message)
        updateUI()
    }

    fun setTerminated() {
        isExecuting = false
        onStateUpdate(IdeaTerminalExecutionState.TERMINATED, "Execution terminated by user")
        updateUI()
    }

    private fun updateUI() {
        ApplicationManager.getApplication().invokeLater {
            val currentText = stringWriter.toString()
            onTextUpdate(currentText, !isExecuting)
        }
    }

    fun getContent(): String = stringWriter.toString()
    
    fun clear() {
        stringWriter.buffer.setLength(0)
    }
}

