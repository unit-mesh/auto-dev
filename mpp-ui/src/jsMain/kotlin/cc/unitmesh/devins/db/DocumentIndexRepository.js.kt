package cc.unitmesh.devins.db

actual class DocumentIndexDatabaseRepository(private val database: DevInsDatabase) : DocumentIndexRepository {
    private val queries = database.documentIndexQueries

    actual override fun save(record: DocumentIndexRecord) {
        queries.insertOrReplace(
            path = record.path,
            hash = record.hash,
            lastModified = record.lastModified,
            status = record.status,
            content = record.content,
            error = record.error,
            indexedAt = record.indexedAt
        )
    }

    actual override fun get(path: String): DocumentIndexRecord? {
        val result = queries.selectByPath(path).executeAsOneOrNull() ?: return null
        return DocumentIndexRecord(
            path = result.path,
            hash = result.hash,
            lastModified = result.lastModified,
            status = result.status,
            content = result.content,
            error = result.error,
            indexedAt = result.indexedAt
        )
    }

    actual override fun getAll(): List<DocumentIndexRecord> {
        return queries.selectAll().executeAsList().map { result ->
            DocumentIndexRecord(
                path = result.path,
                hash = result.hash,
                lastModified = result.lastModified,
                status = result.status,
                content = result.content,
                error = result.error,
                indexedAt = result.indexedAt
            )
        }
    }

    actual override fun delete(path: String) {
        queries.deleteByPath(path)
    }

    actual override fun deleteAll() {
        queries.deleteAll()
    }

    actual companion object {
        private var instance: DocumentIndexRepository? = null

        actual fun getInstance(): DocumentIndexRepository {
            if (instance != null) return instance!!
            
            val driverFactory = DatabaseDriverFactory()
            val database = createDatabase(driverFactory)
            instance = DocumentIndexDatabaseRepository(database)
            return instance!!
        }
    }
}
