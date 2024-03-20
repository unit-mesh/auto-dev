package cc.unitmesh.devti.language.compiler.exec

class PrintInsCommand(private val value: String) : InsCommand {
    override fun execute(): String {
        return value
    }
}