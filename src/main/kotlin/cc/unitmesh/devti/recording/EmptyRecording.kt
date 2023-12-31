package cc.unitmesh.devti.recording

class EmptyRecording: Recording {
    override fun write(instruction: RecordingInstruction) {
        // do nothing
    }
}