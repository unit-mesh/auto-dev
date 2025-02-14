package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.devin.InsCommand
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand

class PrintInsCommand(private val value: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.FILE_FUNC

    override suspend fun execute(): String {
        return value
    }
}