package cc.unitmesh.devins.ui.compose.agent

import cc.unitmesh.devins.ui.compose.editor.model.EditorCallbacks

/**
 * Create callbacks for CodingAgent integration
 * Simplified version that works with ComposeRenderer
 */
fun createAgentCallbacks(
    viewModel: CodingAgentViewModel,
    onConfigWarning: () -> Unit
): EditorCallbacks {
    return object : EditorCallbacks {
        override fun onSubmit(text: String) {
            if (text.isBlank()) return

            // Check if agent is already executing
            if (viewModel.isExecuting) {
                println("Agent is already executing, ignoring new task")
                return
            }

            // Execute the task using CodingAgent
            // Pass onConfigWarning callback to be invoked if configuration is required
            viewModel.executeTask(text.trim(), onConfigRequired = onConfigWarning)
        }
    }
}
