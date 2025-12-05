package cc.unitmesh.devti.devins.provider.vcs

data class ShireFileCommit(
    val filename: String,
    val path: String,
    val status: String,
    override val count: Int,
    override val commits: List<ShireGitCommit>
) : CommitModel(count, commits)