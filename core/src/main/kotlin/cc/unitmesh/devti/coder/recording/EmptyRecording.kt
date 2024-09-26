package cc.unitmesh.devti.coder.recording

class EmptyRecording: Recording {
    override fun write(instruction: RecordingInstruction) {
        // do nothing
    }
}