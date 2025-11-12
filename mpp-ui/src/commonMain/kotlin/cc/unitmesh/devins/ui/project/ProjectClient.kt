package cc.unitmesh.devins.ui.project

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ProjectClient - 项目管理客户端
 * 负责与服务端的项目相关 API 交互
 */
class ProjectClient(
    private val baseUrl: String,
    private val httpClient: HttpClient
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private var authToken: String? = null
    
    fun setAuthToken(token: String) {
        authToken = token
    }
    
    /**
     * 获取项目列表
     */
    suspend fun getProjects(): List<Project> {
        val response = httpClient.get("$baseUrl/api/projects") {
            header("Authorization", "Bearer $authToken")
        }
        
        val responseText = response.bodyAsText()
        return json.decodeFromString<List<Project>>(responseText)
    }
    
    /**
     * 创建项目
     */
    suspend fun createProject(request: CreateProjectRequest): Project {
        val response = httpClient.post("$baseUrl/api/projects") {
            header("Authorization", "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProjectRequest.serializer(), request))
        }
        
        val responseText = response.bodyAsText()
        return json.decodeFromString<Project>(responseText)
    }
    
    /**
     * 获取项目详情
     */
    suspend fun getProject(projectId: String): Project {
        val response = httpClient.get("$baseUrl/api/projects/$projectId") {
            header("Authorization", "Bearer $authToken")
        }
        
        val responseText = response.bodyAsText()
        return json.decodeFromString<Project>(responseText)
    }
    
    /**
     * 删除项目
     */
    suspend fun deleteProject(projectId: String) {
        httpClient.delete("$baseUrl/api/projects/$projectId") {
            header("Authorization", "Bearer $authToken")
        }
    }
}

@Serializable
data class Project(
    val id: String,
    val name: String,
    val path: String,
    val description: String? = null,
    val gitUrl: String? = null,
    val gitBranch: String? = null,
    val ownerId: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateProjectRequest(
    val name: String,
    val description: String? = null,
    val gitUrl: String? = null,
    val gitBranch: String? = null,
    val gitUsername: String? = null,
    val gitPassword: String? = null
)

