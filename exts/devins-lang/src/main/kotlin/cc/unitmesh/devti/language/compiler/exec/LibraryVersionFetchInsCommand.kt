package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class LibraryVersionFetchInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.LIBRARY_VERSION_FETCH

    override suspend fun execute(): String? {
        val (type, name) = parseProp(prop)
        return when (type) {
            "npm" -> fetchNpmVersion(name)
            "maven" -> fetchMavenVersion(name)
            else -> "Unsupported type: $type"
        }
    }

    private fun parseProp(prop: String): Pair<String, String> {
        val parts = prop.split(":", limit = 2)
        return if (parts.size == 2) Pair(parts[0].trim(), parts[1].trim()) else Pair("", "")
    }

    private suspend fun fetchNpmVersion(pkg: String): String? = withContext(Dispatchers.IO) {
        val url = URL("https://registry.npmjs.org/$pkg/latest")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        return@withContext if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().readText()
            // Extract version from JSON
            val regex = "\"version\"\s*:\s*\"([^"]+)\"".toRegex()
            regex.find(body)?.groupValues?.get(1) ?: "Not found"
        } else {
            "npm fetch failed: ${conn.responseCode}"
        }
    }

    private suspend fun fetchMavenVersion(pkg: String): String? = withContext(Dispatchers.IO) {
        // pkg format: groupId:artifactId
        val parts = pkg.split(":")
        if (parts.size != 2) return@withContext "Invalid maven format. Use groupId:artifactId"
        val groupId = parts[0].replace('.', '/')
        val artifactId = parts[1]
        val url = URL("https://search.maven.org/solrsearch/select?q=g:%22${parts[0]}%22+AND+a:%22$artifactId%22&rows=1&wt=json")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        return@withContext if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().readText()
            val regex = "\"latestVersion\"\s*:\s*\"([^"]+)\"".toRegex()
            regex.find(body)?.groupValues?.get(1) ?: "Not found"
        } else {
            "maven fetch failed: ${conn.responseCode}"
        }
    }
}
