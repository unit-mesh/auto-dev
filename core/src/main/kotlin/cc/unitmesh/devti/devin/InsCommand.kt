package cc.unitmesh.devti.devin

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand

interface InsCommand {
    val commandName: BuiltinCommand
    suspend fun execute(): String?
}

enum class InsCommandStatus {
    SUCCESS,
    FAILED,
    RUNNING
}

