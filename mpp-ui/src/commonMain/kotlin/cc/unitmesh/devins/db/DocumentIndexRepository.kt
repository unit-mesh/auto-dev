package cc.unitmesh.devins.db

import kotlinx.serialization.Serializable

@Serializable
data class DocumentIndexRecord(
    val path: String,
    val hash: String,
    val lastModified: Long,
    val status: String,
    val content: String?,
    val error: String?,
    val indexedAt: Long
)

expect class DocumentIndexRepository {
    fun save(record: DocumentIndexRecord)
    fun get(path: String): DocumentIndexRecord?
    fun getAll(): List<DocumentIndexRecord>
    fun delete(path: String)
    fun deleteAll()

    companion object {
        fun getInstance(): DocumentIndexRepository
    }
}
