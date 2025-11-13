package cc.unitmesh.devins.ui.compose.agent

/**
 * Create callbacks for CodingAgent integration
 * Simplified version that works with ComposeRenderer
 */
fun createAgentCallbacks(
    viewModel: CodingAgentViewModel,
    onConfigWarning: () -> Unit
): cc.unitmesh.devins.editor.EditorCallbacks {
    return object : cc.unitmesh.devins.editor.EditorCallbacks {
        override fun onSubmit(text: String) {
            if (text.isBlank()) return

            // Check if agent is already executing
            if (viewModel.isExecuting) {
                println("Agent is already executing, ignoring new task")
                return
            }

            viewModel.executeTask(text.trim(), onConfigRequired = onConfigWarning)
        }
    }
}
