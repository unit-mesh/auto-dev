package cc.unitmesh.devti.language.compiler.exec

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.language.compiler.error.DEVINS_ERROR
import cc.unitmesh.devti.provider.BuildSystemProvider
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LibraryVersionFetchInsCommand(val myProject: Project, val prop: String) : InsCommand {
    override val commandName: BuiltinCommand = BuiltinCommand.LIBRARY_VERSION_FETCH

    override suspend fun execute(): String? {
        if (prop.isBlank()) {
            return "$DEVINS_ERROR Usage: /library-version-fetch:type:packageName or /library-version-fetch:packageName (auto-detect)"
        }

        val parts = prop.split(":", limit = 2)
        return if (parts.size == 2) {
            // Explicit type specified: npm:react, maven:org.springframework:spring-core
            val type = parts[0].trim()
            val name = parts[1].trim()
            fetchVersionByType(type, name)
        } else {
            // Auto-detect from project context
            val packageName = prop.trim()
            autoDetectAndFetch(packageName)
        }
    }

    private suspend fun autoDetectAndFetch(packageName: String): String {
        val buildSystems = BuildSystemProvider.guess(myProject)
        val results = mutableListOf<String>()

        // Try different package managers based on project context
        val typesToTry = mutableSetOf<String>()
        buildSystems.forEach { context ->
            when {
                context.buildToolName?.lowercase()?.contains("npm") == true -> typesToTry.add("npm")
                context.buildToolName?.lowercase()?.contains("maven") == true -> typesToTry.add("maven")
                context.buildToolName?.lowercase()?.contains("gradle") == true -> typesToTry.add("maven")
                context.languageName?.lowercase()?.contains("javascript") == true -> typesToTry.add("npm")
                context.languageName?.lowercase()?.contains("typescript") == true -> typesToTry.add("npm")
                context.languageName?.lowercase()?.contains("java") == true -> typesToTry.add("maven")
                context.languageName?.lowercase()?.contains("kotlin") == true -> typesToTry.add("maven")
                context.languageName?.lowercase()?.contains("python") == true -> typesToTry.add("pypi")
                context.languageName?.lowercase()?.contains("go") == true -> typesToTry.add("go")
                context.languageName?.lowercase()?.contains("rust") == true -> typesToTry.add("crates")
            }
        }

        // If no specific context found, try common ones
        if (typesToTry.isEmpty()) {
            typesToTry.addAll(listOf("npm", "maven", "pypi"))
        }

        for (type in typesToTry) {
            val result = fetchVersionByType(type, packageName)
            if (!result.startsWith("$DEVINS_ERROR") && !result.contains("failed")) {
                results.add("$type: $result")
            }
        }

        return if (results.isNotEmpty()) {
            "Library versions for '$packageName':\n${results.joinToString("\n")}"
        } else {
            "$DEVINS_ERROR No versions found for '$packageName' in any supported registry"
        }
    }

    private suspend fun fetchVersionByType(type: String, name: String): String {
        return try {
            when (type.lowercase()) {
                "npm", "npmjs" -> fetchNpmVersion(name)
                "maven" -> fetchMavenVersion(name)
                "pypi", "pip", "python" -> fetchPypiVersion(name)
                "go", "golang" -> fetchGoVersion(name)
                "crates", "rust", "cargo" -> fetchCratesVersion(name)
                "nuget", ".net", "dotnet" -> fetchNugetVersion(name)
                else -> "$DEVINS_ERROR Unsupported package manager: $type. Supported: npm, maven, pypi, go, crates, nuget"
            }
        } catch (e: Exception) {
            "$DEVINS_ERROR Failed to fetch $type package '$name': ${e.message}"
        }
    }

    private suspend fun fetchNpmVersion(pkg: String): String = withContext(Dispatchers.IO) {
        val url = URL("https://registry.npmjs.org/${URLEncoder.encode(pkg, "UTF-8")}/latest")
        val result = makeHttpRequest(url)
        return@withContext if (result.first == 200) {
            extractJsonField(result.second, "version") ?: "Version not found"
        } else {
            "NPM package '$pkg' not found (${result.first})"
        }
    }

    private suspend fun fetchMavenVersion(coordinates: String): String = withContext(Dispatchers.IO) {
        val parts = coordinates.split(":")
        val (groupId, artifactId) = when (parts.size) {
            1 -> {
                // Just artifact name, search across all groups
                val searchUrl = URL("https://search.maven.org/solrsearch/select?q=a:%22${URLEncoder.encode(parts[0], "UTF-8")}%22&rows=1&wt=json")
                val result = makeHttpRequest(searchUrl)
                return@withContext if (result.first == 200) {
                    val docs = extractJsonArrayField(result.second, "response.docs")
                    if (docs?.isNotEmpty() == true) {
                        val doc = docs.first().jsonObject
                        val g = doc["g"]?.jsonPrimitive?.content ?: "unknown"
                        val a = doc["a"]?.jsonPrimitive?.content ?: "unknown"
                        val v = doc["latestVersion"]?.jsonPrimitive?.content ?: "unknown"
                        "$g:$a:$v"
                    } else "Maven artifact '$coordinates' not found"
                } else "Maven search failed (${result.first})"
            }
            2 -> Pair(parts[0], parts[1])
            else -> return@withContext "$DEVINS_ERROR Invalid Maven coordinates. Use 'artifactId' or 'groupId:artifactId'"
        }
        
        val url = URL("https://search.maven.org/solrsearch/select?q=g:%22${URLEncoder.encode(groupId, "UTF-8")}%22+AND+a:%22${URLEncoder.encode(artifactId, "UTF-8")}%22&rows=1&wt=json")
        val result = makeHttpRequest(url)
        return@withContext if (result.first == 200) {
            val docs = extractJsonArrayField(result.second, "response.docs")
            if (docs?.isNotEmpty() == true) {
                docs.first().jsonObject["latestVersion"]?.jsonPrimitive?.content ?: "Version not found"
            } else "Maven artifact '$coordinates' not found"
        } else {
            "Maven search failed (${result.first})"
        }
    }

    private suspend fun fetchPypiVersion(pkg: String): String = withContext(Dispatchers.IO) {
        val url = URL("https://pypi.org/pypi/${URLEncoder.encode(pkg, "UTF-8")}/json")
        val result = makeHttpRequest(url)
        return@withContext if (result.first == 200) {
            extractJsonField(result.second, "info.version") ?: "Version not found"
        } else {
            "PyPI package '$pkg' not found (${result.first})"
        }
    }

    private suspend fun fetchGoVersion(module: String): String = withContext(Dispatchers.IO) {
        // Go modules use proxy.golang.org
        val url = URL("https://proxy.golang.org/${URLEncoder.encode(module, "UTF-8")}/@latest")
        val result = makeHttpRequest(url)
        return@withContext if (result.first == 200) {
            extractJsonField(result.second, "Version") ?: "Version not found"
        } else {
            "Go module '$module' not found (${result.first})"
        }
    }

    private suspend fun fetchCratesVersion(crateName: String): String = withContext(Dispatchers.IO) {
        val url = URL("https://crates.io/api/v1/crates/${URLEncoder.encode(crateName, "UTF-8")}")
        val result = makeHttpRequest(url)
        return@withContext if (result.first == 200) {
            extractJsonField(result.second, "crate.max_version") ?: "Version not found"
        } else {
            "Rust crate '$crateName' not found (${result.first})"
        }
    }

    private suspend fun fetchNugetVersion(pkg: String): String = withContext(Dispatchers.IO) {
        val url = URL("https://api.nuget.org/v3-flatcontainer/${URLEncoder.encode(pkg.lowercase(), "UTF-8")}/index.json")
        val result = makeHttpRequest(url)
        return@withContext if (result.first == 200) {
            val versions = Json.parseToJsonElement(result.second).jsonObject["versions"]?.jsonArray
            versions?.lastOrNull()?.jsonPrimitive?.content ?: "Version not found"
        } else {
            "NuGet package '$pkg' not found (${result.first})"
        }
    }

    private fun makeHttpRequest(url: URL): Pair<Int, String> {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("User-Agent", "AutoDev LibraryVersionFetch")
        
        return try {
            val responseCode = conn.responseCode
            val body = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            Pair(responseCode, body)
        } finally {
            conn.disconnect()
        }
    }

    private fun extractJsonField(jsonText: String, fieldPath: String): String? {
        return try {
            val jsonElement = Json.parseToJsonElement(jsonText)
            val fields = fieldPath.split(".")
            var current: JsonElement = jsonElement
            
            for (field in fields) {
                current = current.jsonObject[field] ?: return null
            }
            
            current.jsonPrimitive.content
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJsonArrayField(jsonText: String, fieldPath: String): JsonArray? {
        return try {
            val jsonElement = Json.parseToJsonElement(jsonText)
            val fields = fieldPath.split(".")
            var current: JsonElement = jsonElement
            
            for (field in fields) {
                current = current.jsonObject[field] ?: return null
            }
            
            current.jsonArray
        } catch (e: Exception) {
            null
        }
    }
}
