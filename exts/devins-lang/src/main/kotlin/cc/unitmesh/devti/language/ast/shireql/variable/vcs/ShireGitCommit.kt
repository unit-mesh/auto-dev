package cc.unitmesh.devti.language.ast.shireql.variable.vcs

sealed class GitEntity

// Base class for models containing commits
sealed class CommitModel(
    open val count: Int,
    open val commits: List<ShireGitCommit>
) : GitEntity()

data class ShireGitCommit(
    val hash: String,
    val authorName: String,
    val authorEmail: String,
    val authorDate: Long,
    val committerName: String,
    val committerEmail: String,
    val committerDate: Long,
    val message: String,
    val fullMessage: String
) : GitEntity()

