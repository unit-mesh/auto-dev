package cc.unitmesh.devins.ui.project

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ProjectViewModel - 项目视图模型
 * 管理项目列表和项目操作
 */
class ProjectViewModel(
    private val projectClient: ProjectClient
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 项目列表
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    
    // 当前选中的项目
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * 加载项目列表
     */
    suspend fun loadProjects() {
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            val projects = projectClient.getProjects()
            _projects.value = projects
        } catch (e: Exception) {
            _errorMessage.value = "Load projects error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 创建项目
     */
    suspend fun createProject(request: CreateProjectRequest): Project? {
        _isLoading.value = true
        _errorMessage.value = null
        
        return try {
            val project = projectClient.createProject(request)
            loadProjects() // 刷新列表
            project
        } catch (e: Exception) {
            _errorMessage.value = "Create project error: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 选择项目
     */
    fun selectProject(project: Project) {
        _currentProject.value = project
    }
    
    /**
     * 删除项目
     */
    suspend fun deleteProject(projectId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            projectClient.deleteProject(projectId)
            loadProjects() // 刷新列表
        } catch (e: Exception) {
            _errorMessage.value = "Delete project error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun onCleared() {
        viewModelScope.cancel()
    }
}

