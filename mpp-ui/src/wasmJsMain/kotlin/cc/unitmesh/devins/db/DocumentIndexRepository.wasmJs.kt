package cc.unitmesh.devins.db

actual class DocumentIndexRepository(private val database: DevInsDatabase) {
    private val queries = database.documentIndexQueries

    actual fun save(record: DocumentIndexRecord) {
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

    actual fun get(path: String): DocumentIndexRecord? {
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

    actual fun getAll(): List<DocumentIndexRecord> {
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

    actual fun delete(path: String) {
        queries.deleteByPath(path)
    }

    actual fun deleteAll() {
        queries.deleteAll()
    }

    actual companion object {
        private var instance: DocumentIndexRepository? = null

        actual fun getInstance(): DocumentIndexRepository {
            return instance ?: run {
                val driverFactory = DatabaseDriverFactory()
                val database = createDatabase(driverFactory)
                DocumentIndexRepository(database).also { instance = it }
            }
        }
    }
}
