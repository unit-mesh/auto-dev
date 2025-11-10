package cc.unitmesh.server.command

class ScannerCommand() {
    private var args: MutableList<String> = mutableListOf()
    private var dbUrl: String = ""
    private var jarName: String = ""

    private fun java(): String {
        return "java"
    }

    fun file(filename: String): ScannerCommand {
        this.jarName = filename
        return this
    }

    fun dbUrl(dbUrl: String): ScannerCommand {
        this.dbUrl = dbUrl
        return this
    }

    fun path(path: String): ScannerCommand {
        this.args += "--path=$path"
        return this
    }

    fun systemId(systemId: String): ScannerCommand {
        this.args += "--system-id=$systemId"
        return this
    }

    fun language(language: String): ScannerCommand {
        this.args += "--language=${language.lowercase()}"
        return this
    }

    fun getCommand(): List<String> {
        val arguments = mutableListOf(java(), "-jar", "-Ddburl=$dbUrl?useSSL=false", this.jarName)
        arguments.addAll(this.args)
        return arguments
    }
}
