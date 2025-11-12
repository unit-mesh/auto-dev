package cc.unitmesh.devins.ui.task

import cc.unitmesh.agent.RemoteAgentEvent
import cc.unitmesh.devins.ui.project.Project
import cc.unitmesh.devins.ui.remote.RemoteAgentClient
import cc.unitmesh.devins.ui.remote.RemoteAgentRequest
import cc.unitmesh.devins.ui.session.SessionClient
import cc.unitmesh.session.Session
import cc.unitmesh.session.SessionMetadata
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * TaskViewModel - 任务视图模型
 * 管理任务创建和执行，集成 RemoteAgentClient
 */
class TaskViewModel(
    private val sessionClient: SessionClient,
    private val remoteAgentClient: RemoteAgentClient
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 当前项目
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()
    
    // 任务列表（基于 Session）
    private val _tasks = MutableStateFlow<List<Session>>(emptyList())
    val tasks: StateFlow<List<Session>> = _tasks.asStateFlow()
    
    // 当前执行的任务
    private val _currentTask = MutableStateFlow<Session?>(null)
    val currentTask: StateFlow<Session?> = _currentTask.asStateFlow()
    
    // Agent 事件流
    private val _agentEvents = MutableStateFlow<List<RemoteAgentEvent>>(emptyList())
    val agentEvents: StateFlow<List<RemoteAgentEvent>> = _agentEvents.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 执行状态
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()
    
    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var executionJob: Job? = null
    
    /**
     * 设置当前项目
     */
    fun setCurrentProject(project: Project) {
        _currentProject.value = project
        viewModelScope.launch {
            loadTasksForProject(project.id)
        }
    }
    
    /**
     * 加载项目的任务列表
     */
    private suspend fun loadTasksForProject(projectId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            val sessions = sessionClient.getSessions()
            _tasks.value = sessions.filter { it.projectId == projectId }
        } catch (e: Exception) {
            _errorMessage.value = "Load tasks error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 创建任务
     */
    suspend fun createTask(
        taskDescription: String,
        maxIterations: Int = 20
    ): Session? {
        val project = _currentProject.value ?: return null
        
        _isLoading.value = true
        _errorMessage.value = null
        
        return try {
            val session = sessionClient.createSession(
                projectId = project.id,
                task = taskDescription,
                metadata = SessionMetadata(maxIterations = maxIterations)
            )
            
            // 刷新任务列表
            loadTasksForProject(project.id)
            
            session
        } catch (e: Exception) {
            _errorMessage.value = "Create task error: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 执行任务（使用 RemoteAgentClient）
     */
    fun executeTask(session: Session) {
        val project = _currentProject.value ?: return
        
        _currentTask.value = session
        _agentEvents.value = emptyList()
        _isExecuting.value = true
        _errorMessage.value = null
        
        executionJob?.cancel()
        executionJob = viewModelScope.launch {
            try {
                val request = RemoteAgentRequest(
                    projectId = project.id,
                    task = session.task,
                    gitUrl = project.gitUrl,
                    branch = project.gitBranch
                )
                
                remoteAgentClient.executeStream(request).collect { event ->
                    // 追加事件
                    _agentEvents.value = _agentEvents.value + event
                    
                    // 如果是完成事件，停止执行
                    if (event is RemoteAgentEvent.Complete) {
                        _isExecuting.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Execute task error: ${e.message}"
                _isExecuting.value = false
            }
        }
    }
    
    /**
     * 停止任务执行
     */
    fun stopExecution() {
        executionJob?.cancel()
        executionJob = null
        _isExecuting.value = false
    }
    
    /**
     * 清除当前任务
     */
    fun clearCurrentTask() {
        stopExecution()
        _currentTask.value = null
        _agentEvents.value = emptyList()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun onCleared() {
        stopExecution()
        viewModelScope.cancel()
    }
}

