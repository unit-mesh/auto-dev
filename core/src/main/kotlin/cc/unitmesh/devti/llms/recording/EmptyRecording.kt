package cc.unitmesh.devti.llms.recording

class EmptyRecording: Recording {
    override fun write(instruction: RecordingInstruction) {
        // do nothing
    }
}