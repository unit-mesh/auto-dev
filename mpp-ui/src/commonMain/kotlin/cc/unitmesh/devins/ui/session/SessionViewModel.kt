package cc.unitmesh.devins.ui.session

import cc.unitmesh.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * SessionViewModel - 会话视图模型
 * 管理会话状态、用户认证和事件订阅
 */
class SessionViewModel(
    private val sessionClient: SessionClient
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 认证状态
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()
    
    // 会话列表
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()
    
    private val _activeSessions = MutableStateFlow<List<Session>>(emptyList())
    val activeSessions: StateFlow<List<Session>> = _activeSessions.asStateFlow()
    
    // 当前会话
    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()
    
    // 会话事件
    private val _sessionEvents = MutableStateFlow<List<SessionEventEnvelope>>(emptyList())
    val sessionEvents: StateFlow<List<SessionEventEnvelope>> = _sessionEvents.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var subscriptionJob: Job? = null
    
    /**
     * 登录
     */
    suspend fun login(username: String, password: String): Boolean {
        _isLoading.value = true
        _errorMessage.value = null
        
        return try {
            val response = sessionClient.login(username, password)
            if (response.success) {
                _isAuthenticated.value = true
                _currentUser.value = response.username
                
                // 自动加载会话列表
                loadSessions()
                true
            } else {
                _errorMessage.value = response.message ?: "Login failed"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Login error: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 注册
     */
    suspend fun register(username: String, password: String): Boolean {
        _isLoading.value = true
        _errorMessage.value = null
        
        return try {
            val response = sessionClient.register(username, password)
            if (response.success) {
                _isAuthenticated.value = true
                _currentUser.value = response.username
                
                // 自动加载会话列表
                loadSessions()
                true
            } else {
                _errorMessage.value = response.message ?: "Registration failed"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Registration error: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 登出
     */
    suspend fun logout() {
        try {
            sessionClient.logout()
        } catch (e: Exception) {
            // Ignore logout errors
        }
        
        _isAuthenticated.value = false
        _currentUser.value = null
        _sessions.value = emptyList()
        _activeSessions.value = emptyList()
        _currentSession.value = null
        _sessionEvents.value = emptyList()
        
        // 取消订阅
        subscriptionJob?.cancel()
        subscriptionJob = null
    }
    
    /**
     * 创建新会话
     */
    suspend fun createSession(projectId: String, task: String, metadata: SessionMetadata? = null): Session? {
        _isLoading.value = true
        _errorMessage.value = null
        
        return try {
            val session = sessionClient.createSession(projectId, task, metadata)
            loadSessions() // 刷新列表
            session
        } catch (e: Exception) {
            _errorMessage.value = "Create session error: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 加载会话列表
     */
    suspend fun loadSessions() {
        if (!_isAuthenticated.value) return
        
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            val sessions = sessionClient.getSessions()
            _sessions.value = sessions
            
            val activeSessions = sessionClient.getActiveSessions()
            _activeSessions.value = activeSessions
        } catch (e: Exception) {
            _errorMessage.value = "Load sessions error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 加入会话（订阅事件）
     */
    suspend fun joinSession(sessionId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            // 加载会话详情
            val session = sessionClient.getSession(sessionId)
            _currentSession.value = session
            
            // 加载历史状态
            val state = sessionClient.getSessionState(sessionId)
            _sessionEvents.value = state.events
            
            // 订阅实时事件
            subscriptionJob?.cancel()
            subscriptionJob = viewModelScope.launch {
                try {
                    sessionClient.subscribeToSession(sessionId).collect { envelope ->
                        // 追加事件到列表
                        _sessionEvents.value = _sessionEvents.value + envelope
                        
                        // 解析事件并更新会话状态
                        updateSessionFromEvent(envelope)
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Subscription error: ${e.message}"
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Join session error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 离开会话（取消订阅）
     */
    fun leaveSession() {
        subscriptionJob?.cancel()
        subscriptionJob = null
        _currentSession.value = null
        _sessionEvents.value = emptyList()
    }
    
    /**
     * 启动会话执行
     */
    suspend fun executeSession(sessionId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            sessionClient.executeSession(sessionId)
            // 更新会话状态为 RUNNING
            _currentSession.value = _currentSession.value?.copy(status = SessionStatus.RUNNING)
        } catch (e: Exception) {
            _errorMessage.value = "Execute session error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 删除会话
     */
    suspend fun deleteSession(sessionId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        try {
            sessionClient.deleteSession(sessionId)
            loadSessions() // 刷新列表
        } catch (e: Exception) {
            _errorMessage.value = "Delete session error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 从事件更新会话状态
     */
    private fun updateSessionFromEvent(envelope: SessionEventEnvelope) {
        val currentSession = _currentSession.value ?: return
        
        // 根据事件类型更新会话状态
        when (envelope.eventType) {
            "complete" -> {
                // 解析完成事件，判断是否成功
                // 简化处理：假设 complete 事件表示成功
                _currentSession.value = currentSession.copy(
                    status = SessionStatus.COMPLETED,
                    updatedAt = envelope.timestamp
                )
            }
            "error" -> {
                _currentSession.value = currentSession.copy(
                    status = SessionStatus.FAILED,
                    updatedAt = envelope.timestamp
                )
            }
            "iteration" -> {
                if (currentSession.status == SessionStatus.PENDING) {
                    _currentSession.value = currentSession.copy(
                        status = SessionStatus.RUNNING,
                        updatedAt = envelope.timestamp
                    )
                }
            }
        }
    }
    
    /**
     * 清理资源
     */
    fun onCleared() {
        subscriptionJob?.cancel()
        viewModelScope.cancel()
    }
}

