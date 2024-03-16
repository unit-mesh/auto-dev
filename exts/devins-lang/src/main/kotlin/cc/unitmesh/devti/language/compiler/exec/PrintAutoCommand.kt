package cc.unitmesh.devti.language.compiler.exec

class PrintAutoCommand(private val value: String) : AutoCommand {
    override fun execute(): String {
        return value
    }
}