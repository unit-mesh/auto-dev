package cc.unitmesh.devti.recording

interface Recording {
    fun write(instruction: RecordingInstruction)
}
