package cc.unitmesh.viewer.web

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PlantUML Preview") {
        PlantUmlPreview()
    }
}

@Preview
@Composable
fun PlantUmlPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                PlantUmlRenderer(
                    code = """
                        @startuml
                        actor User
                        participant "CodeReviewAgentManager" as Manager
                        participant "CodeReviewAgent" as Agent
                        participant "LLM/Service" as LLM
                        
                        User -> Manager: submitReview(agent, task)
                        Manager -> Manager: generateReviewPlan()
                        Manager -> Agent: Execute Phase 1
                        Agent -> LLM: Request analysis
                        LLM --> Agent: Return findings
                        Manager -> Manager: generateFixSuggestions()
                        Manager --> User: Return artifacts
                        @enduml
                    """.trimIndent(),
                    isDarkTheme = true,
                    modifier = Modifier.fillMaxSize(),
                    onRenderComplete = { success, message ->
                        println("PlantUML render completed: $success - $message")
                    }
                )
            }
        }
    }
}
