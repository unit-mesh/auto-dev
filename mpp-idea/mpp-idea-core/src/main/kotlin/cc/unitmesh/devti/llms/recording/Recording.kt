package cc.unitmesh.devti.llms.recording

interface Recording {
    fun write(instruction: RecordingInstruction)
}
