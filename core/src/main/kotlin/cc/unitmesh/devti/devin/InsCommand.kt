package cc.unitmesh.devti.devin

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand

interface InsCommand {
    val commandName: BuiltinCommand
    fun isApplicable(): Boolean {
        return true
    }

    suspend fun execute(): String?
}

enum class InsCommandStatus {
    SUCCESS,
    FAILED,
    RUNNING
}

