package cc.unitmesh.devti.recording

import kotlinx.serialization.Serializable

@Serializable
data class RecordingInstruction(
    val instruction: String,
    val output: String,
)