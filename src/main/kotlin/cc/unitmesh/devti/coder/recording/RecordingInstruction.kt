package cc.unitmesh.devti.coder.recording

import kotlinx.serialization.Serializable

@Serializable
data class RecordingInstruction(
    val instruction: String,
    val output: String,
)