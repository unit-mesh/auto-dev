package cc.unitmesh.devti.coder.recording

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class JsonlRecording(val project: Project) : Recording {
    private val recordingPath: Path = Path.of(project.guessProjectDir()!!.path, "recording.jsonl")
    override fun write(instruction: RecordingInstruction) {
        if (!recordingPath.toFile().exists()) {
            recordingPath.toFile().createNewFile()
        }

        recordingPath.toFile().appendText(Json.encodeToString(instruction) + "\n")
    }
}

