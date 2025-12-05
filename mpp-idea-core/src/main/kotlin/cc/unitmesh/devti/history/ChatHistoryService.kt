package cc.unitmesh.devti.history

import cc.unitmesh.devti.llms.custom.Message
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import jetbrains.exodus.entitystore.PersistentEntityStore
import jetbrains.exodus.entitystore.PersistentEntityStores
import jetbrains.exodus.env.Environment
import jetbrains.exodus.env.Environments
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Paths
import java.util.*

@Service(Service.Level.PROJECT)
class ChatHistoryService(private val project: Project) : Disposable {
    private val storeName = "chatHistory"
    private val entityType = "ChatSession"

    private val environment: Environment by lazy {
        val projectBasePath = project.basePath ?: throw IllegalStateException("Project base path is null")
        val dbPath = Paths.get(projectBasePath, ".idea", "autodev", "history").toString()
        Environments.newInstance(dbPath)
    }

    private val entityStore: PersistentEntityStore by lazy {
        PersistentEntityStores.newInstance(environment, storeName)
    }

    fun saveSession(name: String, messages: List<Message>): ChatSessionHistory {
        val sessionId = UUID.randomUUID().toString()
        val history = ChatSessionHistory(sessionId, name, messages, System.currentTimeMillis())
        val jsonHistory = Json.encodeToString(history)

        entityStore.executeInTransaction { txn ->
            val entity = txn.newEntity(entityType)
            entity.setProperty("id", sessionId)
            entity.setProperty("name", name)
            entity.setBlobString("messages", jsonHistory)
            entity.setProperty("createdAt", history.createdAt)
        }
        return history
    }

    fun getSession(sessionId: String): ChatSessionHistory? {
        var history: ChatSessionHistory? = null
        entityStore.executeInReadonlyTransaction { txn ->
            val entity = txn.find(entityType, "id", sessionId).firstOrNull()
            entity?.let {
                val jsonHistory = it.getBlobString("messages")
                if (jsonHistory != null) {
                    history = Json.decodeFromString<ChatSessionHistory>(jsonHistory)
                }
            }
        }
        return history
    }

    fun getAllSessions(): List<ChatSessionHistory> {
        val histories = mutableListOf<ChatSessionHistory>()
        entityStore.executeInReadonlyTransaction { txn ->
            txn.getAll(entityType).forEach { entity ->
                val jsonHistory = entity.getBlobString("messages")
                if (jsonHistory != null) {
                    try {
                        histories.add(Json.decodeFromString<ChatSessionHistory>(jsonHistory))
                    } catch (e: Exception) {
                        // Log error or handle corrupted data
                        println("Error decoding session history: ${entity.getProperty("id")}, ${e.message}")
                    }
                }
            }
        }
        // Sort by creation date, newest first
        return histories.sortedByDescending { it.createdAt }
    }

    fun deleteSession(sessionId: String): Boolean {
        var deleted = false
        entityStore.executeInTransaction { txn ->
            val entity = txn.find(entityType, "id", sessionId).firstOrNull()
            entity?.let {
                deleted = it.delete()
            }
        }
        return deleted
    }

    override fun dispose() {
        entityStore.close()
        environment.close()
    }
}