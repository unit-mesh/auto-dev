package cc.unitmesh.devti.devin

import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand

interface InsCommand {
    val commandName: BuiltinCommand

    /**
     * Check if the command is applicable , especially for the binary command, like ripgrep.
     */
    fun isApplicable(): Boolean = true

    /**
     * Execute the command and return the result.
     */
    suspend fun execute(): String?
}

enum class InsCommandStatus {
    SUCCESS,
    FAILED,
    RUNNING
}

