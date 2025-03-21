package cc.unitmesh.devti.sketch.run

import com.intellij.openapi.application.ApplicationManager
import java.io.StringWriter
import java.io.Writer

/**
 * A Writer implementation that updates UI components based on the output stream.
 * Uses callbacks to abstract UI interactions.
 */
class UIUpdatingWriter(
    private val onTextUpdate: (String, Boolean) -> Unit,
    private val onPanelUpdate: (String, Boolean) -> Unit,
    private val checkCollapsed: () -> Boolean,
    private val expandPanel: () -> Unit
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
        updateUI()
    }

    fun updateUI() {
        ApplicationManager.getApplication().invokeLater {
            val currentText = stringWriter.toString()
            onTextUpdate(currentText, !isExecuting)

            if (checkCollapsed()) {
                expandPanel()
            }

            if (isExecuting) {
                onPanelUpdate("Execution Results (Running...)", isExecuting)
            } else {
                if (currentText.contains("EXIT_CODE: 0")) {
                    onPanelUpdate("Execution Results (Success)", isExecuting)
                } else if (currentText.contains("EXIT_CODE:")) {
                    val exitCodePattern = "EXIT_CODE: (\\d+)".toRegex()
                    val match = exitCodePattern.find(currentText)
                    val exitCode = match?.groupValues?.get(1) ?: "Error"
                    onPanelUpdate("Execution Results (Error: $exitCode)", isExecuting)
                } else {
                    onPanelUpdate("Execution Results", isExecuting)
                }
            }
        }
    }

    fun getContent(): String = stringWriter.toString()
}