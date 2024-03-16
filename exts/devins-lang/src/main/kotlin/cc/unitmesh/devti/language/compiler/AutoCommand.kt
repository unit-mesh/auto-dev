package cc.unitmesh.devti.language.compiler

interface AutoCommand {
    fun execute(): String?
}

class PrintAutoCommand(private val value: String) : AutoCommand {
    override fun execute(): String {
        return value
    }
}
