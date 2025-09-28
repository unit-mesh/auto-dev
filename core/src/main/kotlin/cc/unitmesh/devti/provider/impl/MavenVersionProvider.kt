package cc.unitmesh.devti.provider.impl

import cc.unitmesh.devti.provider.LibraryVersionProvider
import cc.unitmesh.devti.provider.VersionRequest
import cc.unitmesh.devti.provider.VersionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MavenVersionProvider : LibraryVersionProvider() {
    override val packageType: String = "maven"
    
    override suspend fun fetchVersion(request: VersionRequest): VersionResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val coordinates = request.name
            val parts = coordinates.split(":")
            
            when (parts.size) {
                1 -> {
                    // Just artifact name, search across all groups
                    searchMavenArtifact(coordinates)
                }
                2 -> {
                    // groupId:artifactId format
                    val (groupId, artifactId) = parts
                    fetchMavenVersion(groupId, artifactId, coordinates)
                }
                else -> {
                    VersionResult.error(coordinates, packageType, "Invalid Maven coordinates. Use 'artifactId' or 'groupId:artifactId'")
                }
            }
        } catch (e: Exception) {
            VersionResult.error(request.name, packageType, "Failed to fetch Maven artifact '${request.name}': ${e.message}")
        }
    }
    
    private fun searchMavenArtifact(artifactId: String): VersionResult {
        val searchUrl = URL("https://search.maven.org/solrsearch/select?q=a:%22${URLEncoder.encode(artifactId, "UTF-8")}%22&rows=1&wt=json")
        val result = makeHttpRequest(searchUrl)
        
        return if (result.first == 200) {
            val docs = extractJsonArrayField(result.second, "response.docs")
            if (docs?.isNotEmpty() == true) {
                val doc = docs.first().jsonObject
                val g = doc["g"]?.jsonPrimitive?.content ?: "unknown"
                val a = doc["a"]?.jsonPrimitive?.content ?: "unknown"
                val v = doc["latestVersion"]?.jsonPrimitive?.content ?: "unknown"
                VersionResult.success(artifactId, packageType, "$g:$a:$v")
            } else {
                VersionResult.error(artifactId, packageType, "Maven artifact '$artifactId' not found")
            }
        } else {
            VersionResult.error(artifactId, packageType, "Maven search failed (${result.first})")
        }
    }
    
    private fun fetchMavenVersion(groupId: String, artifactId: String, coordinates: String): VersionResult {
        val url = URL("https://search.maven.org/solrsearch/select?q=g:%22${URLEncoder.encode(groupId, "UTF-8")}%22+AND+a:%22${URLEncoder.encode(artifactId, "UTF-8")}%22&rows=1&wt=json")
        val result = makeHttpRequest(url)
        
        return if (result.first == 200) {
            val docs = extractJsonArrayField(result.second, "response.docs")
            if (docs?.isNotEmpty() == true) {
                val version = docs.first().jsonObject["latestVersion"]?.jsonPrimitive?.content
                if (version != null) {
                    VersionResult.success(coordinates, packageType, version)
                } else {
                    VersionResult.error(coordinates, packageType, "Version not found")
                }
            } else {
                VersionResult.error(coordinates, packageType, "Maven artifact '$coordinates' not found")
            }
        } else {
            VersionResult.error(coordinates, packageType, "Maven search failed (${result.first})")
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
    
    private fun extractJsonArrayField(jsonText: String, fieldPath: String): kotlinx.serialization.json.JsonArray? {
        return try {
            val jsonElement = Json.parseToJsonElement(jsonText)
            val fields = fieldPath.split(".")
            var current = jsonElement
            
            for (field in fields) {
                current = current.jsonObject[field] ?: return null
            }
            
            current.jsonArray
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getImplementationClassName(): String? {
        return MavenVersionProvider::class.java.name
    }
}
