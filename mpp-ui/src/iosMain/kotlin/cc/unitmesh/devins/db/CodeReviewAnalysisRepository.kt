package cc.unitmesh.devins.db

import cc.unitmesh.devins.ui.compose.agent.codereview.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * CodeReviewAnalysisRepository - iOS 实现
 */
actual class CodeReviewAnalysisRepository(private val database: DevInsDatabase) {
    private val queries = database.codeReviewAnalysisQueries
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    actual fun saveAnalysisResult(remoteUrl: String?, projectPath: String, commitHash: String, progress: AIAnalysisProgress) {
        // Try to find existing record using remoteUrl first, then projectPath
        val existing = if (!remoteUrl.isNullOrBlank()) {
            queries.selectByRemoteAndCommit(remoteUrl, commitHash).executeAsOneOrNull()
        } else {
            queries.selectByProjectAndCommit(projectPath, commitHash).executeAsOneOrNull()
        }
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        
        if (existing != null) {
            // Update existing record
            queries.update(
                stage = progress.stage.name,
                lintOutput = progress.lintOutput,
                lintResults = json.encodeToString(progress.lintResults),
                modifiedCodeRanges = json.encodeToString(progress.modifiedCodeRanges),
                analysisOutput = progress.analysisOutput,
                planOutput = progress.planOutput,
                fixOutput = progress.fixOutput,
                updatedAt = now,
                id = existing.id
            )
        } else {
            // Insert new record with remoteUrl as primary identifier
            val identifier = if (!remoteUrl.isNullOrBlank()) remoteUrl else projectPath
            val id = "${identifier}_${commitHash}_${now}"
            queries.insert(
                id = id,
                remoteUrl = remoteUrl ?: "",
                projectPath = projectPath,
                commitHash = commitHash,
                stage = progress.stage.name,
                lintOutput = progress.lintOutput,
                lintResults = json.encodeToString(progress.lintResults),
                modifiedCodeRanges = json.encodeToString(progress.modifiedCodeRanges),
                analysisOutput = progress.analysisOutput,
                planOutput = progress.planOutput,
                fixOutput = progress.fixOutput,
                createdAt = now,
                updatedAt = now
            )
        }
    }
    
    actual fun getAnalysisResult(remoteUrl: String?, projectPath: String, commitHash: String): AIAnalysisProgress? {
        // Try remoteUrl first, fallback to projectPath
        val record = if (!remoteUrl.isNullOrBlank()) {
            queries.selectByRemoteAndCommit(remoteUrl, commitHash).executeAsOneOrNull()
        } else {
            null
        } ?: queries.selectByProjectAndCommit(projectPath, commitHash).executeAsOneOrNull()
            ?: return null
        
        return AIAnalysisProgress(
            stage = AnalysisStage.valueOf(record.stage),
            lintOutput = record.lintOutput,
            lintResults = if (record.lintResults.isNotBlank()) {
                json.decodeFromString(record.lintResults)
            } else {
                emptyList()
            },
            modifiedCodeRanges = if (record.modifiedCodeRanges.isNotBlank()) {
                json.decodeFromString(record.modifiedCodeRanges)
            } else {
                emptyMap()
            },
            analysisOutput = record.analysisOutput,
            planOutput = record.planOutput,
            fixOutput = record.fixOutput
        )
    }
    
    actual fun getAnalysisResultsByProject(remoteUrl: String?, projectPath: String): List<Pair<String, AIAnalysisProgress>> {
        // Try remoteUrl first, fallback to projectPath
        val records = if (!remoteUrl.isNullOrBlank()) {
            queries.selectByRemote(remoteUrl).executeAsList()
        } else {
            queries.selectByProject(projectPath).executeAsList()
        }
        
        return records.mapNotNull { record ->
            try {
                val progress = AIAnalysisProgress(
                    stage = AnalysisStage.valueOf(record.stage),
                    lintOutput = record.lintOutput,
                    lintResults = if (record.lintResults.isNotBlank()) {
                        json.decodeFromString(record.lintResults)
                    } else {
                        emptyList()
                    },
                    modifiedCodeRanges = if (record.modifiedCodeRanges.isNotBlank()) {
                        json.decodeFromString(record.modifiedCodeRanges)
                    } else {
                        emptyMap()
                    },
                    analysisOutput = record.analysisOutput,
                    planOutput = record.planOutput,
                    fixOutput = record.fixOutput
                )
                record.commitHash to progress
            } catch (e: Exception) {
                null
            }
        }
    }
    
    actual fun deleteAnalysisResult(remoteUrl: String?, projectPath: String, commitHash: String) {
        // Try remoteUrl first, fallback to projectPath
        if (!remoteUrl.isNullOrBlank()) {
            queries.deleteByRemoteAndCommit(remoteUrl, commitHash)
        } else {
            queries.deleteByProjectAndCommit(projectPath, commitHash)
        }
    }
    
    actual fun deleteOldAnalysis(keepCount: Long) {
        queries.deleteOldAnalysis(keepCount)
    }
    
    actual fun deleteAll() {
        queries.deleteAll()
    }
    
    actual companion object {
        private var instance: CodeReviewAnalysisRepository? = null
        
        actual fun getInstance(): CodeReviewAnalysisRepository {
            return instance ?: run {
                val driverFactory = DatabaseDriverFactory()
                val database = createDatabase(driverFactory)
                CodeReviewAnalysisRepository(database).also { instance = it }
            }
        }
    }
}

