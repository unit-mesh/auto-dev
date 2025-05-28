package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llm2.model.CopilotModel
import cc.unitmesh.devti.llm2.model.CopilotModelsResponse
import com.intellij.openapi.diagnostic.Logger
import com.jayway.jsonpath.JsonPath
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Utility class to detect if GitHub Copilot is installed and configured
 */
object GithubCopilotDetector {
    private val logger = Logger.getInstance(GithubCopilotDetector::class.java)

    // Cache for supported models to ensure updates happen only once
    private var cachedModels: CopilotModelsResponse? = null
    private var modelsLastUpdated: Long = 0

    // Constants for model types
    const val TYPE_EMBEDDING = "embedding"
    const val TYPE_COMPLETION = "completion"

    /**
     * Check if GitHub Copilot is installed and configured by looking for an oauth_token
     * in the GitHub Copilot config file
     *
     * @return true if GitHub Copilot is installed and configured, false otherwise
     */
    fun isGithubCopilotConfigured(): Boolean {
        return extractOauthToken() != null
    }

    /**
     * Extract the OAuth token from the GitHub Copilot config file
     *
     * @return the OAuth token, or null if not found
     */
    fun extractOauthToken(): String? {
        val configDir = getConfigDir()
        val appsFile = configDir.resolve("apps.json")

        if (!appsFile.exists()) {
            return null
        }

        return appsFile.readText().let {
            val arr: List<String>? = JsonPath.parse(it)?.read("$..oauth_token")
            arr?.lastOrNull()
        }
    }

    /**
     * Get the GitHub Copilot config directory
     *
     * @return the config directory
     */
    private fun getConfigDir(): File {
        // mac only TODO: windows
        val homeDir = System.getProperty("user.home")
        return File("${homeDir}/.config/github-copilot/")
    }

    /**
     * Get the supported models from GitHub Copilot
     * Uses cached models if available and not expired
     *
     * @param forceRefresh Force refresh the models even if cache is available
     * @return the list of supported models, or null if not found
     */
    fun getSupportedModels(forceRefresh: Boolean = false): CopilotModelsResponse? {
        // Return cached models if available and not expired (cache for 1 hour)
        val currentTime = System.currentTimeMillis()
        if (!forceRefresh && cachedModels != null && (currentTime - modelsLastUpdated < 3600000)) {
            return cachedModels
        }

        val oauthToken = extractOauthToken()
        if (oauthToken == null) {
            logger.warn("Failed to get supported models: OAuth token not found")
            return null
        }

        val client = OkHttpClient.Builder().build()

        // First, get an API token
        val apiToken = requestApiToken(client, oauthToken)
        if (apiToken == null) {
            logger.warn("Failed to get supported models: API token not found")
            return null
        }

        // Then, get the supported models
        val request = Request.Builder()
            .url("https://api.githubcopilot.com/models")
            .addHeader("Authorization", "Bearer ${apiToken.token}")
            .addHeader("Editor-Version", "Neovim/0.6.1")
            .addHeader("Content-Type", "application/json")
            .addHeader("Copilot-Integration-Id", "vscode-chat")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody == null) {
                    logger.warn("Failed to get supported models: Response body is empty")
                    return null
                }

                try {
                    // Parse the JSON response
                    val json = Json { ignoreUnknownKeys = true }
                    println(responseBody)
                    val parsedResponse = json.decodeFromString<CopilotModelsResponse>(responseBody)

                    // Filter models to only include those with enabled policy state
                    // Keep models where policy is null (backward compatibility) or policy.state is "enabled"
                    val filteredModels = parsedResponse.data.filter { model ->
                        model.policy?.state == "enabled" || model.policy == null
                    }

                    val originalCount = parsedResponse.data.size
                    val filteredCount = filteredModels.size
                    if (originalCount != filteredCount) {
                        logger.info("Filtered GitHub Copilot models: $originalCount -> $filteredCount (removed ${originalCount - filteredCount} disabled models)")
                    }

                    val models = CopilotModelsResponse(data = filteredModels)

                    // Update cache
                    cachedModels = models
                    modelsLastUpdated = currentTime

                    models
                } catch (e: Exception) {
                    logger.warn("Failed to parse supported models response", e)
                    null
                }
            } else {
                logger.warn("Failed to get supported models: ${response.code}")
                null
            }
        } catch (e: Exception) {
            logger.warn("Exception while getting supported models", e)
            null
        }
    }

    /**
     * Request an API token from GitHub Copilot
     *
     * @param client the HTTP client
     * @param oauthToken the OAuth token
     * @return the API token, or null if not found
     */
    private fun requestApiToken(client: OkHttpClient, oauthToken: String): ApiToken? {
        val request = Request.Builder()
            .url("https://api.github.com/copilot_internal/v2/token")
            .addHeader("Authorization", "token $oauthToken")
            .addHeader("Accept", "application/json")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: throw IllegalStateException("Response body is empty")
                val tokenResponse = JsonPath.parse(responseBody) ?: throw IllegalStateException("Failed to parse response")
                val token: String = tokenResponse.read("$.token") ?: throw IllegalStateException("Failed to parse token")
                val expiresAt: Int = tokenResponse.read("$.expires_at") ?: throw IllegalStateException("Failed to parse expiresAt")
                ApiToken(token, expiresAt)
            } else {
                logger.warn("Failed to get API token: ${response.code}")
                null
            }
        } catch (e: Exception) {
            logger.warn("Exception while getting API token", e)
            null
        }
    }

    /**
     * API token for GitHub Copilot
     */
    private data class ApiToken(
        val token: String,
        val expiresAt: Int
    )
}
