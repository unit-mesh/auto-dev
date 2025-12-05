package cc.unitmesh.devti.provider.impl

import cc.unitmesh.devti.provider.LibraryVersionProvider
import cc.unitmesh.devti.provider.VersionRequest
import cc.unitmesh.devti.provider.VersionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class NpmVersionProvider : LibraryVersionProvider() {
    override val packageType: String = "npm"
    
    override fun canHandle(packageType: String): Boolean {
        return packageType.lowercase() in listOf("npm", "npmjs", "node")
    }
    
    override suspend fun fetchVersion(request: VersionRequest): VersionResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("https://registry.npmjs.org/${URLEncoder.encode(request.name, "UTF-8")}/latest")
            val result = makeHttpRequest(url)
            
            if (result.first == 200) {
                val version = extractJsonField(result.second, "version")
                if (version != null) {
                    VersionResult.success(request.name, packageType, version)
                } else {
                    VersionResult.error(request.name, packageType, "Version not found in response")
                }
            } else {
                VersionResult.error(request.name, packageType, "NPM package '${request.name}' not found (${result.first})")
            }
        } catch (e: Exception) {
            VersionResult.error(request.name, packageType, "Failed to fetch NPM package '${request.name}': ${e.message}")
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
            var current = jsonElement
            
            for (field in fields) {
                current = current.jsonObject[field] ?: return null
            }
            
            current.jsonPrimitive.content
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getImplementationClassName(): String? {
        return NpmVersionProvider::class.java.name
    }
}
