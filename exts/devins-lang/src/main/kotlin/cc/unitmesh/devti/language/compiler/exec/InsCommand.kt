package cc.unitmesh.devti.language.compiler.exec

interface InsCommand {
    suspend fun execute(): String?
}

