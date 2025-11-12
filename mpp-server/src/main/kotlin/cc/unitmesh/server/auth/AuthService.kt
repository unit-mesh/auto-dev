package cc.unitmesh.server.auth

import cc.unitmesh.session.User
import cc.unitmesh.session.LoginRequest
import cc.unitmesh.session.LoginResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * AuthService - 简单的用户认证服务
 * 使用内存存储，支持基本的用户名密码认证
 */
class AuthService {
    // 用户存储：username -> User
    private val userStore = ConcurrentHashMap<String, User>()
    
    // 活跃会话：token -> username
    private val activeSessions = ConcurrentHashMap<String, String>()
    
    init {
        // 创建默认用户（仅用于测试）
        val defaultUser = User(
            username = "admin",
            passwordHash = hashPassword("admin123"),
            createdAt = System.currentTimeMillis()
        )
        userStore[defaultUser.username] = defaultUser
        logger.info { "Created default user: admin" }
    }
    
    /**
     * 用户注册
     */
    fun register(username: String, password: String): LoginResponse {
        if (username.isBlank() || password.isBlank()) {
            return LoginResponse(
                success = false,
                message = "Username and password cannot be empty"
            )
        }
        
        if (userStore.containsKey(username)) {
            return LoginResponse(
                success = false,
                message = "Username already exists"
            )
        }
        
        val user = User(
            username = username,
            passwordHash = hashPassword(password),
            createdAt = System.currentTimeMillis()
        )
        
        userStore[username] = user
        logger.info { "Registered new user: $username" }
        
        // 自动登录
        return login(username, password)
    }
    
    /**
     * 用户登录
     */
    fun login(username: String, password: String): LoginResponse {
        val user = userStore[username]
        
        if (user == null) {
            logger.warn { "Login failed: user not found - $username" }
            return LoginResponse(
                success = false,
                message = "Invalid username or password"
            )
        }
        
        val passwordHash = hashPassword(password)
        if (user.passwordHash != passwordHash) {
            logger.warn { "Login failed: invalid password for user - $username" }
            return LoginResponse(
                success = false,
                message = "Invalid username or password"
            )
        }
        
        // 生成简单的 token（实际应使用 JWT）
        val token = generateToken(username)
        activeSessions[token] = username
        
        logger.info { "User logged in: $username" }
        
        return LoginResponse(
            success = true,
            username = username,
            token = token,
            message = "Login successful"
        )
    }
    
    /**
     * 验证 token
     */
    fun validateToken(token: String): String? {
        return activeSessions[token]
    }
    
    /**
     * 登出
     */
    fun logout(token: String) {
        val username = activeSessions.remove(token)
        if (username != null) {
            logger.info { "User logged out: $username" }
        }
    }
    
    /**
     * 获取用户信息
     */
    fun getUser(username: String): User? {
        return userStore[username]
    }
    
    /**
     * 哈希密码（使用 SHA-256）
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 生成简单的 token
     * 实际生产环境应使用 JWT
     */
    private fun generateToken(username: String): String {
        val timestamp = System.currentTimeMillis()
        val data = "$username:$timestamp"
        return hashPassword(data)
    }
}

