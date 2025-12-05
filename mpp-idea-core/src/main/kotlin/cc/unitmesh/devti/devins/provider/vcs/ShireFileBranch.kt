package cc.unitmesh.devti.devins.provider.vcs

data class ShireFileBranch(
    val name: String,
    override val count: Int,
    override val commits: List<ShireGitCommit>
) : CommitModel(count, commits)