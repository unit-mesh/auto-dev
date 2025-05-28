package cc.unitmesh.devti.llm2

import cc.unitmesh.devti.llm2.model.CopilotModel
import cc.unitmesh.devti.llm2.model.CopilotModelsResponse
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * 管理GitHub Copilot模型的服务
 * 负责初始化时获取模型列表并提供缓存访问
 */
@Service
class GithubCopilotManager() : Disposable {
    private val logger = Logger.getInstance(GithubCopilotManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = OkHttpClient.Builder().build()

    // 缓存的模型列表
    private var modelsCache: List<CopilotModel>? = null

    // 最后一次更新时间
    private var lastUpdateTime: Long = 0

    // 初始化状态
    private var isInitialized = false
    private var isInitializing = false

    // 定义监听器接口
    interface ModelsUpdateListener {
        fun onModelsUpdated(models: List<CopilotModel>?)
    }

    // 存储注册的监听器
    private val listeners = mutableListOf<ModelsUpdateListener>()

    /**
     * 注册模型更新监听器
     */
    fun addListener(listener: ModelsUpdateListener) {
        listeners.add(listener)
        // 如果已经初始化过，立即通知新的监听器
        if (isInitialized) {
            listener.onModelsUpdated(modelsCache)
        }
    }

    /**
     * 移除模型更新监听器
     */
    fun removeListener(listener: ModelsUpdateListener) {
        listeners.remove(listener)
    }

    /**
     * 通知所有监听器模型已更新
     */
    private fun notifyListeners(models: List<CopilotModel>?) {
        listeners.forEach { it.onModelsUpdated(models) }
    }

    /**
     * 初始化Copilot模型列表
     * 此方法应在启动时调用
     */
    fun initialize() {
        if (isInitialized || isInitializing) return

        isInitializing = true
        scope.launch {
            try {
                if (!GithubCopilotDetector.isGithubCopilotConfigured()) {
                    logger.info("GitHub Copilot not configured, skipping model initialization")
                    return@launch
                }

                logger.info("Initializing GitHub Copilot models...")
                val modelsResponse = withContext(Dispatchers.IO) {
                    GithubCopilotDetector.getSupportedModels(forceRefresh = true)
                }

                if (modelsResponse != null) {
                    val models = modelsResponse.data
                    modelsCache = models
                    lastUpdateTime = System.currentTimeMillis()
                    isInitialized = true
                    logger.info("GitHub Copilot models initialized successfully: ${models.size} models available")

                    // 通知所有监听器
                    notifyListeners(models)
                } else {
                    logger.warn("Failed to initialize GitHub Copilot models")
                }
            } catch (e: Exception) {
                logger.warn("Error initializing GitHub Copilot models", e)
            } finally {
                isInitializing = false
            }
        }
    }

    /**
     * 获取支持的模型列表
     * 如果尚未初始化或强制刷新，则会进行远程请求
     * 否则返回缓存的结果
     *
     * @param forceRefresh 是否强制刷新，默认为false
     * @return 支持的模型列表，如果未初始化或获取失败则返回null
     */
    fun getSupportedModels(forceRefresh: Boolean = false): List<CopilotModel>? {
        if (forceRefresh) {
            // 如果强制刷新，则直接请求新数据
            val freshModelsResponse = GithubCopilotDetector.getSupportedModels(forceRefresh = true)
            if (freshModelsResponse != null) {
                val freshModels = freshModelsResponse.data
                modelsCache = freshModels
                lastUpdateTime = System.currentTimeMillis()
                isInitialized = true

                // 通知所有监听器
                notifyListeners(modelsCache)
            }
            return freshModelsResponse?.data ?: emptyList()
        }

        // 如果已初始化并且缓存存在，直接返回缓存
        return modelsCache
    }

    /**
     * 判断是否已初始化完成
     */
    fun isInitialized(): Boolean = isInitialized


    /**
     * 检查最后一次更新时间，如果超过指定时间则考虑刷新
     * 此方法仅返回是否需要刷新的建议，不进行实际刷新
     *
     * @param maxAgeMs 最大缓存时间（毫秒）
     * @return 如果缓存过期则返回true
     */
    fun isCacheStale(maxAgeMs: Long = 3600000): Boolean { // 默认1小时
        return System.currentTimeMillis() - lastUpdateTime > maxAgeMs
    }

    override fun dispose() {
        // 清理资源
        listeners.clear()
    }

    companion object {
        /**
         * 获取服务实例
         */
        @JvmStatic
        fun getInstance(): GithubCopilotManager {
            return service<GithubCopilotManager>()
        }
    }
}
