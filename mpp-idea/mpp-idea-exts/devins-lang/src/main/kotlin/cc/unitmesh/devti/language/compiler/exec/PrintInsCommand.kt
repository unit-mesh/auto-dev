package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand

class PrintInsCommand(private val value: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.OPEN

    override suspend fun execute(): String {
        return value
    }
}