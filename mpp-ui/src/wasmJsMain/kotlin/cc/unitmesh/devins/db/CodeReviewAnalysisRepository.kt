package cc.unitmesh.devins.db

import cc.unitmesh.devins.ui.compose.agent.codereview.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * CodeReviewAnalysisRepository - WasmJs 实现
 */
actual class CodeReviewAnalysisRepository(private val database: DevInsDatabase) {
    private val queries = database.codeReviewAnalysisQueries
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    actual fun saveAnalysisResult(projectPath: String, commitHash: String, progress: AIAnalysisProgress) {
        val existing = queries.selectByProjectAndCommit(projectPath, commitHash).executeAsOneOrNull()
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        
        if (existing != null) {
            // Update existing record
            queries.update(
                stage = progress.stage.name,
                lintOutput = progress.lintOutput,
                lintResults = json.encodeToString(progress.lintResults),
                modifiedCodeRanges = json.encodeToString(progress.modifiedCodeRanges),
                analysisOutput = progress.analysisOutput,
                fixOutput = progress.fixOutput,
                updatedAt = now,
                id = existing.id
            )
        } else {
            // Insert new record
            val id = "${projectPath}_${commitHash}_${now}"
            queries.insert(
                id = id,
                projectPath = projectPath,
                commitHash = commitHash,
                stage = progress.stage.name,
                lintOutput = progress.lintOutput,
                lintResults = json.encodeToString(progress.lintResults),
                modifiedCodeRanges = json.encodeToString(progress.modifiedCodeRanges),
                analysisOutput = progress.analysisOutput,
                fixOutput = progress.fixOutput,
                createdAt = now,
                updatedAt = now
            )
        }
    }
    
    actual fun getAnalysisResult(projectPath: String, commitHash: String): AIAnalysisProgress? {
        val record = queries.selectByProjectAndCommit(projectPath, commitHash).executeAsOneOrNull()
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
            fixOutput = record.fixOutput
        )
    }
    
    actual fun getAnalysisResultsByProject(projectPath: String): List<Pair<String, AIAnalysisProgress>> {
        return queries.selectByProject(projectPath).executeAsList().mapNotNull { record ->
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
                    fixOutput = record.fixOutput
                )
                record.commitHash to progress
            } catch (e: Exception) {
                null
            }
        }
    }
    
    actual fun deleteAnalysisResult(projectPath: String, commitHash: String) {
        queries.deleteByProjectAndCommit(projectPath, commitHash)
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

