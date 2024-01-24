package cc.unitmesh.devti.flow

interface TaskFlow {
    fun clarify(): String
    fun design(context: Any): String {
        return ""
    }
    fun execute(): String {
        return ""
    }
}