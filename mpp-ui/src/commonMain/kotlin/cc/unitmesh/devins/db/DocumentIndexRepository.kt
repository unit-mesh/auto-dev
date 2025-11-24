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

interface DocumentIndexRepository {
    fun save(record: DocumentIndexRecord)
    fun get(path: String): DocumentIndexRecord?
    fun getAll(): List<DocumentIndexRecord>
    fun delete(path: String)
    fun deleteAll()
}

expect class DocumentIndexDatabaseRepository : DocumentIndexRepository {
    override fun save(record: DocumentIndexRecord)
    override fun get(path: String): DocumentIndexRecord?
    override fun getAll(): List<DocumentIndexRecord>
    override fun delete(path: String)
    override fun deleteAll()

    companion object {
        fun getInstance(): DocumentIndexRepository
    }
}
