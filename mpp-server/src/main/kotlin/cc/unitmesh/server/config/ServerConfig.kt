package cc.unitmesh.server.config

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val projects: ProjectsConfig = ProjectsConfig(),
    val llm: LLMConfig = LLMConfig()
) {
    companion object {
        fun load(): ServerConfig {
            val host = System.getenv("SERVER_HOST") ?: "0.0.0.0"
            val port = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080
            val projectsRoot = System.getenv("PROJECTS_ROOT") ?: System.getProperty("user.home")

            val llmProvider = System.getenv("LLM_PROVIDER") ?: "openai"
            val llmModel = System.getenv("LLM_MODEL") ?: "gpt-4"
            val llmApiKey = System.getenv("LLM_API_KEY") ?: ""
            val llmBaseUrl = System.getenv("LLM_BASE_URL") ?: ""

            return ServerConfig(
                host = host,
                port = port,
                projects = ProjectsConfig(rootPath = projectsRoot),
                llm = LLMConfig(
                    provider = llmProvider,
                    modelName = llmModel,
                    apiKey = llmApiKey,
                    baseUrl = llmBaseUrl
                )
            )
        }
    }
}

@Serializable
data class ProjectsConfig(
    val rootPath: String = System.getProperty("user.home"),
    val allowedProjects: List<String> = emptyList()
)

@Serializable
data class LLMConfig(
    val provider: String = "openai",
    val modelName: String = "gpt-4",
    val apiKey: String = "",
    val baseUrl: String = ""
)

